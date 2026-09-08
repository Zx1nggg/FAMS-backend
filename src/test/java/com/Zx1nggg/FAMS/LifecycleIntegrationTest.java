package com.Zx1nggg.FAMS;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.*;
import com.Zx1nggg.FAMS.modules.base.mapper.*;
import com.Zx1nggg.FAMS.modules.base.dto.StockingDTO;
import com.Zx1nggg.FAMS.modules.base.service.IStockingService;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.*;
import com.Zx1nggg.FAMS.modules.lifecycle.dto.HarvestRecordDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.*;
import com.Zx1nggg.FAMS.modules.lifecycle.service.*;
import com.Zx1nggg.FAMS.modules.log.entity.*;
import com.baomidou.mybatisplus.annotation.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Real mapper SQL, transactions and row locks on an isolated H2 database (MySQL mode). */
@SpringBootTest(properties = {
        "app.scheduling.enabled=false", "spring.datasource.url=jdbc:h2:mem:lifecycle;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
class LifecycleIntegrationTest {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("jwt.secret", () -> UUID.randomUUID().toString() + UUID.randomUUID());
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired IHarvestRecordService harvest;
    @Autowired IStockingService stockingService;
    @Autowired com.Zx1nggg.FAMS.modules.base.service.IPurchaseBatchService purchaseBatchService;
    @Autowired PurchaseBatchMapper batches;
    @Autowired SupplierMapper suppliers;
    @Autowired SeedlingDictMapper seedlings;
    @Autowired PondMapper ponds;
    @Autowired StockingMapper stockings;
    @Autowired HarvestRecordMapper harvests;
    @Autowired LifecycleAccessService access;
    @Autowired IBatchGrowthLogService growth;
    @Autowired IPatrolLogService patrols;
    @Autowired com.Zx1nggg.FAMS.modules.log.service.IPondFeedLogService feeds;
    @Autowired com.Zx1nggg.FAMS.modules.regulator.service.IInspectionRecordService inspections;
    @Autowired com.Zx1nggg.FAMS.modules.log.service.IAlarmRecordService alarms;
    @Autowired IPondTaskService tasks;
    @Autowired com.Zx1nggg.FAMS.modules.system.service.IUserService userService;
    @Autowired com.Zx1nggg.FAMS.modules.system.service.IRegistrationApplicationService registrations;
    @Autowired com.Zx1nggg.FAMS.modules.system.mapper.RegistrationApplicationMapper registrationMapper;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @Autowired com.Zx1nggg.FAMS.modules.iot.simulator.HourlyAggregator hourlyAggregator;
    @Autowired com.Zx1nggg.FAMS.modules.iot.simulator.IotDataSimulator simulator;
    @Autowired com.Zx1nggg.FAMS.modules.iot.service.IIotSensorDataService sensorData;
    @Autowired com.Zx1nggg.FAMS.modules.base.service.IFarmService farms;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.Zx1nggg.FAMS.security.service.UserFarmCacheService farmCache;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    org.springframework.data.redis.core.StringRedisTemplate redis;

    @Test void farmRestoreOnlyRestoresItsOwnCascadeDeletion() {
        jdbc.update("UPDATE t_pond SET is_deleted=1 WHERE id=102");
        farms.batchDelete(List.of(10L));
        assertThat(ponds.selectById(101L)).isNull();
        farms.restore(List.of(10L));
        assertThat(farms.getById(10L)).isNotNull();
        assertThat(ponds.selectById(101L)).isNotNull();
        assertThat(ponds.selectById(102L)).isNull();
        assertThat(jdbc.queryForObject("SELECT delete_batch FROM t_pond WHERE id=101", String.class)).isNull();
    }
    @Test void oldDeletionWithoutBatchMarkerDoesNotRestorePonds() {
        jdbc.update("UPDATE t_farm SET is_deleted=1 WHERE id=10");
        jdbc.update("UPDATE t_pond SET is_deleted=1 WHERE farm_id=10");
        farms.restore(List.of(10L));
        assertThat(farms.getById(10L)).isNotNull();
        assertThat(ponds.selectById(101L)).isNull();
    }
    @Test void crossUserRestoreIsRejectedAndWholeCascadeRollsBackOnFailure() {
        jdbc.update("INSERT INTO t_farm(id,user_id,is_deleted) VALUES(20,2,1)");
        assertThatThrownBy(() -> farms.restore(List.of(20L))).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM t_farm WHERE id=20", Integer.class)).isEqualTo(1);
        jdbc.execute("ALTER TABLE t_farm ADD CONSTRAINT prevent_delete CHECK(is_deleted=0 OR id=20)");
        assertThatThrownBy(() -> farms.batchDelete(List.of(10L))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(ponds.selectById(101L)).isNotNull();
        assertThat(farms.getById(10L).getDeleteBatch()).isNull();
    }

    @Test void deletedForeignFarmCannotBypassCascadeAuthorization() {
        jdbc.update("INSERT INTO t_farm(id,user_id,is_deleted) VALUES(20,2,1)");
        assertThatThrownBy(() -> farms.batchDelete(List.of(10L, 20L))).isInstanceOf(BusinessException.class);
        assertThat(ponds.selectById(201L)).isNotNull();
        assertThat(farms.getById(10L)).isNotNull();
    }
    @Test void repeatedFarmDeletePreservesRestoreMarker() {
        farms.batchDelete(List.of(10L));
        String marker = jdbc.queryForObject("SELECT delete_batch FROM t_farm WHERE id=10", String.class);
        farms.batchDelete(List.of(10L));
        assertThat(jdbc.queryForObject("SELECT delete_batch FROM t_farm WHERE id=10", String.class)).isEqualTo(marker);
        farms.restore(List.of(10L));
        assertThat(ponds.selectById(101L)).isNotNull();
    }

    @Test void farmOwnerMustExistAndAccountWithFarmHistoryCannotBeDeleted() {
        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().setAttribute("currentUserType", "ADMIN");
        var dto = new com.Zx1nggg.FAMS.modules.base.dto.FarmDTO(); dto.setFarmName("owner check"); dto.setUserId(99L);
        assertThatThrownBy(() -> farms.create(dto, 1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> farms.update(10L, dto)).isInstanceOf(BusinessException.class);
        assertThat(farms.getById(10L).getUserId()).isEqualTo(1L);
        var owner = new com.Zx1nggg.FAMS.modules.system.entity.User(); owner.setId(99L); userService.save(owner);
        var farm = farms.create(dto, 1L); assertThat(farm.getUserId()).isEqualTo(99L);
        assertThatThrownBy(() -> userService.deleteUsers(List.of(99L))).isInstanceOf(BusinessException.class);
        assertThat(userService.getById(99L)).isNotNull();
    }

    @BeforeEach void setup() {
        jdbc.execute("DROP ALL OBJECTS");
        // Build an isolated persistence fixture from mapped columns; MySQL migrations are verified separately.
        for (Class<?> type : List.of(Farm.class, Pond.class, PurchaseBatch.class, Stocking.class, SeedlingDict.class,
                Supplier.class, SopTemplate.class, HarvestRecord.class, PondTask.class, BatchGrowthLog.class,
                PatrolLog.class, PondFeedLog.class, AlarmRecord.class, AlarmActionLog.class,
                com.Zx1nggg.FAMS.modules.regulator.entity.InspectionRecord.class,
                com.Zx1nggg.FAMS.modules.system.entity.User.class,
                com.Zx1nggg.FAMS.modules.iot.entity.IotSensorData.class,
                com.Zx1nggg.FAMS.modules.system.entity.RegistrationApplication.class)) createTable(type);
        jdbc.execute("ALTER TABLE t_harvest_record ADD COLUMN active_record INT AS (CASE WHEN is_deleted=0 THEN 1 ELSE NULL END)");
        jdbc.execute("CREATE UNIQUE INDEX uk_harvest_batch_pond_active ON t_harvest_record(batch_no, pond_id, active_record)");
        jdbc.execute("ALTER TABLE sys_registration_application ADD COLUMN pending_phone VARCHAR(20) AS (CASE WHEN status=0 THEN phone ELSE NULL END)");
        jdbc.execute("CREATE UNIQUE INDEX uk_registration_pending_phone ON sys_registration_application(pending_phone)");
        jdbc.execute("CREATE TABLE t_supplier_seedling (supplier_id BIGINT NOT NULL, seedling_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(supplier_id, seedling_id))");
        bindFarmer(10L);
        for (long id : List.of(101L, 102L, 201L)) {
            Pond pond = new Pond(); pond.setId(id); pond.setFarmId(id == 201L ? 20L : 10L); pond.setPondName("test pond"); ponds.insert(pond);
        }
        PurchaseBatch batch = new PurchaseBatch(); batch.setId(1L); batch.setBatchNo("TEST-BATCH"); batch.setFarmId(10L);
        batch.setUnitQty(10); batch.setDensityPerUnit(100); batch.setEstimatedTotalQty(1000);
        batch.setTotalAmount(new BigDecimal("1000")); batch.setBatchStatus((byte) 2); batch.setPurchaseDate(LocalDate.of(2026,1,1));
        batches.insert(batch);
        jdbc.update("INSERT INTO t_farm(id, user_id, farm_name) VALUES(10,1,'test farm')");
        addStocking(101L, 4); addStocking(102L, 6);
    }
    @AfterEach void clearContext() { RequestContextHolder.resetRequestAttributes(); }
    private void bindFarmer(Long farmId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("currentUserId", 1L); request.setAttribute("currentUserType", "FARMER"); request.setAttribute("currentFarmId", farmId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    private void addStocking(Long pondId, int units) {
        Stocking value = new Stocking(); value.setBatchId(1L); value.setPondId(pondId); value.setStockedUnits(units);
        value.setStockedQty(units*100); value.setStockingDate(LocalDate.of(2026,1,2)); stockings.insert(value);
    }
    private HarvestRecordDTO dto(Long pondId) {
        HarvestRecordDTO dto = new HarvestRecordDTO(); dto.setBatchNo("TEST-BATCH"); dto.setPondId(pondId);
        dto.setHarvestDate(LocalDate.of(2026,2,1)); dto.setActualTotalWeightKg(new BigDecimal("100"));
        dto.setUnitPrice(new BigDecimal("20")); dto.setFeedCost(BigDecimal.ZERO); dto.setMedicineCost(BigDecimal.ZERO);
        dto.setBuyerName("test buyer");
        return dto;
    }
    @Test void eachPondSettlesSeparatelyAndLastPondClosesBatch() {
        var first = harvest.create(dto(101L));
        assertThat(first.getSeedlingCost()).isEqualByComparingTo("400");
        assertThat(first.getTotalCost()).isEqualByComparingTo("400");
        assertThat(batches.selectById(1L).getBatchStatus()).isEqualTo((byte) 2);
        var second = harvest.create(dto(102L));
        assertThat(second.getSeedlingCost()).isEqualByComparingTo("600");
        assertThat(batches.selectById(1L).getBatchStatus()).isEqualTo((byte) 3);
        assertThat(harvest.list()).hasSize(2);
    }
    @Test void duplicateSettlementDoesNotChangeData() {
        harvest.create(dto(101L));
        assertThatThrownBy(() -> harvest.create(dto(101L))).isInstanceOf(BusinessException.class);
        assertThat(harvest.list()).hasSize(1);
    }
    @Test void clearingSettlementPriceAlsoClearsPersistedRevenueAndProfit() {
        var record = harvest.create(dto(101L));
        assertThat(harvest.getById(record.getId()).getTotalRevenue()).isEqualByComparingTo("2000");
        var changed = dto(101L); changed.setUnitPrice(null); changed.setSettlementStatus(0);
        harvest.update(record.getId(), changed);
        var persisted = harvest.getById(record.getId());
        assertThat(persisted.getUnitPrice()).isNull(); assertThat(persisted.getTotalRevenue()).isNull(); assertThat(persisted.getNetProfit()).isNull();
        assertThat(persisted.getSettlementStatus()).isZero();
    }
    @Test void clearingFeedPriceDoesNotLeaveAnOldCalculatedAmount() {
        var dto = new com.Zx1nggg.FAMS.modules.log.dto.PondFeedLogDTO(); dto.setPondId(101L); dto.setLogDate(LocalDate.of(2026,1,3));
        dto.setFeedBrand("test feed"); dto.setFeedAmount(BigDecimal.TEN); dto.setFeedUnitPrice(new BigDecimal("2"));
        var record = feeds.create(dto); assertThat(record.getFeedTotalAmount()).isEqualByComparingTo("20");
        dto.setFeedUnitPrice(null); feeds.update(record.getId(), dto);
        assertThat(feeds.getById(record.getId()).getFeedUnitPrice()).isNull();
        assertThat(feeds.getById(record.getId()).getFeedTotalAmount()).isNull();
    }
    @Test void deletingAndResettlingPreservesSoftDeletedHistory() {
        var first = harvest.create(dto(101L)); harvest.create(dto(102L));
        harvest.batchDelete(List.of(first.getId()));
        assertThat(batches.selectById(1L).getBatchStatus()).isEqualTo((byte) 2);
        harvest.create(dto(101L));
        assertThat(harvest.list()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_harvest_record", Integer.class)).isEqualTo(3);
    }
    @Test void failureAfterInsertRollsBackSettlement() {
        harvest.create(dto(101L));
        jdbc.execute("ALTER TABLE t_purchase_batch ADD CONSTRAINT reject_closed CHECK(batch_status <> 3)");
        assertThatThrownBy(() -> harvest.create(dto(102L))).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(harvest.list()).hasSize(1);
        assertThat(batches.selectById(1L).getBatchStatus()).isEqualTo((byte) 2);
    }
    @Test void concurrentDuplicateSettlementCreatesOnlyOneRecord() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            bindFarmer(10L); start.await();
            try { harvest.create(dto(101L)); return true; }
            catch (BusinessException e) { return false; }
            finally { RequestContextHolder.resetRequestAttributes(); }
        };
        try {
            Future<Boolean> a = executor.submit(attempt), b = executor.submit(attempt); start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            assertThat(harvest.list()).hasSize(1);
        } finally { executor.shutdownNow(); }
    }
    @Test void crossFarmSettlementAndPreviewAreRejected() {
        bindFarmer(20L);
        assertThatThrownBy(() -> harvest.create(dto(101L))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> harvest.preview(1L, 101L)).isInstanceOf(BusinessException.class);
        assertThat(harvest.list()).isEmpty();
    }
    @Test void previewUsesSelectedPond() {
        assertThat(harvest.preview(1L, 102L)).containsEntry("totalStockedQty", 600).containsEntry("pondId", 102L);
    }
    @Test void crossFarmGrowthChartIsRejectedBeforeDataReturned() {
        bindFarmer(20L);
        assertThatThrownBy(() -> growth.getGrowthChart("TEST-BATCH", 101L)).isInstanceOf(BusinessException.class);
    }
    @Test void missingFarmCannotBecomeGlobalQuery() {
        bindFarmer(null);
        assertThatThrownBy(() -> harvest.pageQuery(1,10,null,null,null)).isInstanceOf(BusinessException.class);
    }
    @Test void batchPondAssociationCannotBeForged() {
        assertThatThrownBy(() -> access.requireBatchForPond("TEST-BATCH", 201L, true)).isInstanceOf(BusinessException.class);
    }
    @Test void settledPondCannotReceiveMoreStockingOrGrowthEdits() {
        harvest.create(dto(101L));
        assertThatThrownBy(() -> access.requireBatchForPond("TEST-BATCH", 101L, true)).isInstanceOf(BusinessException.class);
        StockingDTO dto = new StockingDTO(); dto.setBatchId(1L); dto.setPondId(101L); dto.setStockedUnits(1); dto.setStockingDate(LocalDate.of(2026,1,3));
        assertThatThrownBy(() -> stockingService.create(dto)).isInstanceOf(BusinessException.class);
    }
    private void bindRegulator() {
        bindFarmer(null);
        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().setAttribute("currentUserType", "REGULATOR");
    }

    private com.Zx1nggg.FAMS.modules.base.dto.PurchaseBatchDTO purchaseDto(Long supplierId, Long seedlingId) {
        var dto = new com.Zx1nggg.FAMS.modules.base.dto.PurchaseBatchDTO();
        dto.setFarmId(10L); dto.setSupplierId(supplierId); dto.setSeedlingId(seedlingId);
        dto.setPurchaseUnit("袋"); dto.setUnitQty(2); dto.setDensityPerUnit(100);
        dto.setUnitPrice(new BigDecimal("50")); dto.setPurchaseDate(LocalDate.of(2026, 1, 1));
        return dto;
    }

    private void addApprovedOffering(Long supplierId, Long seedlingId) {
        Supplier supplier = new Supplier(); supplier.setId(supplierId); supplier.setSupplierName("approved supplier"); suppliers.insert(supplier);
        SeedlingDict seedling = new SeedlingDict(); seedling.setId(seedlingId); seedling.setCategoryName("approved seedling"); seedlings.insert(seedling);
        suppliers.insertSeedlingOffering(supplierId, seedlingId);
    }

    @Test void farmerCannotSelfApproveQuarantineAndRegulatorApprovalIsAStateTransition() {
        addApprovedOffering(11L, 21L);
        var dto = purchaseDto(11L, 21L);
        dto.setBatchStatus((byte) 1); dto.setQuarantineCertNo("FORGED-BY-CLIENT");
        var created = purchaseBatchService.create(dto);
        assertThat(created.getBatchStatus()).isZero();
        assertThat(created.getQuarantineCertNo()).isNull();
        assertThatThrownBy(() -> purchaseBatchService.approveQuarantine(created.getId(), "QC-REAL"))
                .isInstanceOf(BusinessException.class);

        bindRegulator();
        var approved = purchaseBatchService.approveQuarantine(created.getId(), " QC-REAL ");
        assertThat(approved.getBatchStatus()).isEqualTo((byte) 1);
        assertThat(approved.getQuarantineCertNo()).isEqualTo("QC-REAL");
        assertThat(approved.getQuarantineReviewerId()).isEqualTo(1L);
        assertThat(approved.getQuarantineReviewedAt()).isNotNull();
        assertThatThrownBy(() -> purchaseBatchService.approveQuarantine(created.getId(), "QC-SECOND"))
                .isInstanceOf(BusinessException.class);
    }

    @Test void purchaseMustUseASeedlingActuallyOfferedBySupplier() {
        addApprovedOffering(12L, 22L);
        SeedlingDict unavailable = new SeedlingDict(); unavailable.setId(23L); unavailable.setCategoryName("not offered"); seedlings.insert(unavailable);
        assertThatThrownBy(() -> purchaseBatchService.create(purchaseDto(12L, 23L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未登记供应");
        assertThat(purchaseBatchService.create(purchaseDto(12L, 22L)).getSeedlingName()).isEqualTo("approved seedling");
    }
    @Test void inspectionLifecycleAndValidation() {
        bindRegulator();
        var dto = new com.Zx1nggg.FAMS.modules.regulator.dto.InspectionRecordDTO();
        dto.setFarmId(10L); dto.setPondId(101L); dto.setInspectionDate(LocalDate.now());
        dto.setInspectionType("water_quality"); dto.setResult("unqualified"); dto.setUnqualifiedReason("test finding");
        var record = inspections.create(dto);
        assertThat(record.getInspectorId()).isEqualTo(1L);
        assertThat(record.getRectifyStatus()).isEqualTo("pending");
        var action = new com.Zx1nggg.FAMS.modules.regulator.dto.InspectionRectifyDTO();
        action.setRectifyRemark("verified action"); action.setRectifyStatus("accepted");
        assertThatThrownBy(() -> inspections.rectify(record.getId(), action)).isInstanceOf(BusinessException.class);
        for (String state : List.of("rectifying", "rectified", "accepted")) {
            action.setRectifyStatus(state); inspections.rectify(record.getId(), action);
            assertThat(inspections.detail(record.getId()).getRectifyStatus()).isEqualTo(state);
        }
        assertThatThrownBy(() -> inspections.rectify(record.getId(), action)).isInstanceOf(BusinessException.class);
        assertThat(inspections.stats()).containsEntry("completedRectify", 1L);
        bindFarmer(10L);
        assertThatThrownBy(() -> inspections.detail(record.getId())).isInstanceOf(BusinessException.class);
    }
    @Test void inspectionCannotAttachPondFromAnotherFarm() {
        bindRegulator();
        var dto = new com.Zx1nggg.FAMS.modules.regulator.dto.InspectionRecordDTO();
        dto.setFarmId(10L); dto.setPondId(201L); dto.setInspectionDate(LocalDate.now()); dto.setInspectionType("feed"); dto.setResult("qualified");
        assertThatThrownBy(() -> inspections.create(dto)).isInstanceOf(BusinessException.class);
        assertThat(inspections.list()).isEmpty();
    }
    @Test void inspectionEditCanClearOptionalPond() {
        bindRegulator();
        var dto = new com.Zx1nggg.FAMS.modules.regulator.dto.InspectionRecordDTO();
        dto.setFarmId(10L); dto.setPondId(101L); dto.setInspectionDate(LocalDate.now()); dto.setInspectionType("feed"); dto.setResult("qualified");
        var created = inspections.create(dto); dto.setPondId(null);
        assertThat(inspections.updateRecord(created.getId(), dto).getPondId()).isNull();
    }
    @Test void alarmReopenClearsOldResolutionAndRejectsInvalidTransition() {
        AlarmRecord alarm = new AlarmRecord(); alarm.setFarmId(10L); alarm.setPondId(101L); alarm.setStatus((byte) 0); alarms.save(alarm);
        assertThatThrownBy(() -> alarms.close(alarm.getId(), "invalid")).isInstanceOf(BusinessException.class);
        alarms.resolve(alarm.getId(), "fixed");
        assertThat(alarms.getById(alarm.getId()).getResolvedAt()).isNotNull();
        alarms.reopen(alarm.getId(), "recurrence");
        AlarmRecord result = alarms.getById(alarm.getId());
        assertThat(result.getStatus()).isEqualTo((byte) 0);
        assertThat(result.getResolvedAt()).isNull(); assertThat(result.getResolutionRemark()).isNull(); assertThat(result.getAcknowledgedAt()).isNull();
        bindFarmer(20L);
        assertThatThrownBy(() -> alarms.resolve(alarm.getId(), "other farm")).isInstanceOf(BusinessException.class);
    }
    @Test void alarmLogFailureRollsBackState() {
        AlarmRecord alarm = new AlarmRecord(); alarm.setFarmId(10L); alarm.setPondId(101L); alarm.setStatus((byte) 0); alarms.save(alarm);
        jdbc.execute("ALTER TABLE sys_alarm_action_log ADD CONSTRAINT reject_action CHECK(action_type <> 'RESOLVE')");
        assertThatThrownBy(() -> alarms.resolve(alarm.getId(), "fixed")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(alarms.getById(alarm.getId()).getStatus()).isEqualTo((byte) 0);
    }
    @Test void batchTaskCheckOffRollsBackIfAnyTaskIsUnauthorized() {
        PondTask first = new PondTask(); first.setPondId(101L); first.setStatus((byte) 0); tasks.save(first);
        PondTask other = new PondTask(); other.setPondId(201L); other.setStatus((byte) 0); tasks.save(other);
        assertThatThrownBy(() -> tasks.batchCheckOff(List.of(first.getId(), other.getId()))).isInstanceOf(BusinessException.class);
        assertThat(tasks.getById(first.getId()).getStatus()).isEqualTo((byte) 0);
        tasks.checkOff(first.getId());
        assertThatThrownBy(() -> tasks.checkOff(first.getId())).isInstanceOf(BusinessException.class);
    }
    @Test void passwordChangeChecksOldCredentialAndIncrementsTokenVersion() {
        String original = UUID.randomUUID().toString(), replacement = UUID.randomUUID().toString();
        var user = new com.Zx1nggg.FAMS.modules.system.entity.User(); user.setId(1L); user.setPassword(encoder.encode(original)); user.setAuthVersion(0L); userService.save(user);
        var dto = new com.Zx1nggg.FAMS.modules.system.dto.ChangePasswordDTO(); dto.setOldPassword("invalid"); dto.setNewPassword(replacement);
        assertThatThrownBy(() -> userService.changePassword(1L, dto)).isInstanceOf(BusinessException.class);
        dto.setOldPassword(original); userService.changePassword(1L, dto);
        assertThat(userService.getById(1L).getAuthVersion()).isEqualTo(1L);
        assertThat(encoder.matches(replacement, userService.getById(1L).getPassword())).isTrue();
    }
    @Test void registrationApprovalIsAtomicAndCannotBeRepeated() {
        var app = new com.Zx1nggg.FAMS.modules.system.entity.RegistrationApplication();
        app.setPhone("test-registration"); app.setUsername("display name"); app.setRealName("real name"); app.setFarmName("new farm"); app.setFarmProvince("province"); app.setFarmCity("city"); app.setFarmAddress("street");
        app.setPassword(encoder.encode(UUID.randomUUID().toString())); app.setStatus(0); registrationMapper.insert(app);
        var approval = new com.Zx1nggg.FAMS.modules.system.dto.ApprovalReqDTO(); approval.setStatus(1);
        Long userId = registrations.approveApplication(app.getId(), 99L, approval);
        assertThat(userService.getById(userId).getUsername()).isEqualTo("display name");
        assertThat(userService.getById(userId).getRealName()).isEqualTo("real name");
        assertThat(userService.getById(userId).getFarmId()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT address FROM t_farm WHERE id=?", String.class, userService.getById(userId).getFarmId())).isEqualTo("province city street");
        assertThatThrownBy(() -> registrations.approveApplication(app.getId(), 99L, approval)).isInstanceOf(BusinessException.class);
        assertThat(userService.count()).isEqualTo(1);
    }
    @Test void registrationFarmInsertFailureRollsBackUserCreation() {
        var app = new com.Zx1nggg.FAMS.modules.system.entity.RegistrationApplication(); app.setPhone("test-registration"); app.setUsername("display name"); app.setRealName("real name"); app.setFarmName("rejected farm"); app.setStatus(0);
        app.setPassword(encoder.encode(UUID.randomUUID().toString())); registrationMapper.insert(app);
        jdbc.execute("ALTER TABLE t_farm ADD CONSTRAINT reject_farm CHECK(farm_name <> 'rejected farm')");
        var approval = new com.Zx1nggg.FAMS.modules.system.dto.ApprovalReqDTO(); approval.setStatus(1);
        assertThatThrownBy(() -> registrations.approveApplication(app.getId(), 99L, approval)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(userService.count()).isZero(); assertThat(registrationMapper.selectById(app.getId()).getStatus()).isZero();
    }
    @Test void pendingRegistrationIsUniqueButRejectedHistoryIsRetained() {
        var dto = new com.Zx1nggg.FAMS.modules.system.dto.RegistrationReqDTO();
        dto.setPhone("13900008888"); dto.setPassword(UUID.randomUUID().toString()); dto.setUsername("test user"); dto.setRealName("real name"); dto.setFarmName("test");
        registrations.submitApplication(dto);
        assertThatThrownBy(() -> registrations.submitApplication(dto)).isInstanceOf(BusinessException.class);
        var app = registrations.list().getFirst();
        var duplicate = new com.Zx1nggg.FAMS.modules.system.entity.RegistrationApplication();
        duplicate.setPhone(dto.getPhone()); duplicate.setStatus(0);
        assertThatThrownBy(() -> registrationMapper.insert(duplicate)).isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        var reject = new com.Zx1nggg.FAMS.modules.system.dto.ApprovalReqDTO(); reject.setStatus(2); reject.setReviewComment("correction needed");
        registrations.approveApplication(app.getId(), 99L, reject);
        registrations.submitApplication(dto);
        assertThat(registrations.list()).hasSize(2);
    }
    @Test void registrationDetailsRequireTheLatestApplicationPassword() {
        String password = UUID.randomUUID().toString();
        var dto = new com.Zx1nggg.FAMS.modules.system.dto.RegistrationReqDTO(); dto.setPhone("13900007777"); dto.setPassword(password);
        dto.setUsername("display name"); dto.setRealName("private name"); dto.setFarmName("private farm"); registrations.submitApplication(dto);
        assertThatThrownBy(() -> registrations.queryStatusByPhone(dto.getPhone(), "incorrect")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> registrations.queryStatusByPhone("13900006666", password)).isInstanceOf(BusinessException.class);
        assertThat(registrations.queryStatusByPhone(dto.getPhone(), password).getUsername()).isEqualTo("display name");
        assertThat(registrations.queryStatusByPhone(dto.getPhone(), password).getRealName()).isEqualTo("private name");
        var rejected = new com.Zx1nggg.FAMS.modules.system.dto.ApprovalReqDTO(); rejected.setStatus(2); rejected.setReviewComment("review detail");
        registrations.approveApplication(registrations.list().getFirst().getId(), 99L, rejected);
        assertThat(registrations.queryStatusByPhone(dto.getPhone(), password).getReviewComment()).isEqualTo("review detail");
        String replacement = UUID.randomUUID().toString(); dto.setPassword(replacement); registrations.submitApplication(dto);
        assertThatThrownBy(() -> registrations.queryStatusByPhone(dto.getPhone(), password)).isInstanceOf(BusinessException.class);
        assertThat(registrations.queryStatusByPhone(dto.getPhone(), replacement).getStatus()).isZero();
    }
    @Test void stockingDateCorrectionMovesPendingTasksButPreservesCompletedHistory() {
        var stock = stockings.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Stocking>()
                .eq(Stocking::getPondId, 101L)).getFirst();
        PondTask task = new PondTask(); task.setPondId(101L); task.setBatchNo("TEST-BATCH"); task.setStatus((byte) 0);
        task.setScheduledDate(LocalDate.of(2026,1,3)); tasks.save(task);
        StockingDTO change = new StockingDTO(); change.setBatchId(1L); change.setPondId(101L); change.setStockedUnits(4);
        change.setStockingDate(LocalDate.of(2026,1,5)); stockingService.update(stock.getId(), change);
        assertThat(tasks.getById(task.getId()).getScheduledDate()).isEqualTo(LocalDate.of(2026,1,6));
        tasks.checkOff(task.getId()); change.setStockingDate(LocalDate.of(2026,1,8));
        assertThatThrownBy(() -> stockingService.update(stock.getId(), change)).isInstanceOf(BusinessException.class);
        assertThat(stockings.selectById(stock.getId()).getStockingDate()).isEqualTo(LocalDate.of(2026,1,5));
        assertThat(tasks.getById(task.getId()).getScheduledDate()).isEqualTo(LocalDate.of(2026,1,6));
    }
    @Test void deletingAllStockingReturnsBatchToWarehouseAndRemovesOnlyPendingTasks() {
        PondTask task = new PondTask(); task.setPondId(101L); task.setBatchNo("TEST-BATCH"); task.setStatus((byte) 0); tasks.save(task);
        stockingService.batchDelete(stockings.selectList(null).stream().map(Stocking::getId).toList());
        assertThat(batches.selectById(1L).getBatchStatus()).isEqualTo((byte) 1);
        assertThat(tasks.getById(task.getId())).isNull();
        assertThat(stockings.selectList(null)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_stocking", Integer.class)).isEqualTo(2);
    }
    @Test void overdueTaskCanBeCompletedExactlyOnce() {
        PondTask task = new PondTask(); task.setPondId(101L); task.setBatchNo("TEST-BATCH"); task.setStatus((byte) 2); tasks.save(task);
        tasks.checkOff(task.getId());
        assertThat(tasks.getById(task.getId()).getStatus()).isEqualTo((byte) 1);
        assertThatThrownBy(() -> tasks.checkOff(task.getId())).isInstanceOf(BusinessException.class);
    }
    @Test void hourlyAggregationIgnoresWholeInvalidSamplesAndDoesNotDuplicateAnHour() {
        @SuppressWarnings("unchecked")
        var zset = (org.springframework.data.redis.core.ZSetOperations<String, String>) org.mockito.Mockito.mock(org.springframework.data.redis.core.ZSetOperations.class);
        org.mockito.Mockito.when(redis.opsForZSet()).thenReturn(zset);
        org.mockito.Mockito.when(zset.rangeByScore(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble()))
                .thenReturn(new LinkedHashSet<>(List.of("{\"waterTemp\":20,\"dissolvedOxygen\":6,\"phValue\":7}",
                        "{\"waterTemp\":1000,\"dissolvedOxygen\":\"invalid\",\"phValue\":7}",
                        "{\"waterTemp\":\"NaN\",\"dissolvedOxygen\":6,\"phValue\":7}")));
        hourlyAggregator.aggregate(); hourlyAggregator.aggregate();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_iot_sensor_data", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT water_temp FROM t_iot_sensor_data WHERE pond_id=101", BigDecimal.class)).isEqualByComparingTo("20");
        org.mockito.Mockito.verify(zset, org.mockito.Mockito.times(6)).rangeByScore(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.doubleThat(v -> v.longValue() % 1000 == 999));
    }
    @Test void automaticAlarmRecoveryRollsBackWhenAuditInsertFails() {
        var rule = new AlarmRule(); rule.setAlarmCode("TEST"); rule.setMetricCode("water_temp");
        var alarm = new AlarmRecord(); alarm.setFarmId(10L); alarm.setPondId(101L); alarm.setStatus((byte) 0); alarm.setDedupKey("10:101:TEST:water_temp"); alarms.save(alarm);
        jdbc.execute("ALTER TABLE sys_alarm_action_log ADD CONSTRAINT reject_recovery CHECK(action_type <> 'AUTO_RECOVER')");
        assertThatThrownBy(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(simulator, "recoverAlarm", ponds.selectById(101L), rule))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(alarms.getById(alarm.getId()).getStatus()).isEqualTo((byte) 0);
        assertThat(alarms.getById(alarm.getId()).getRecoveredAt()).isNull();
    }
    @Test void staleIotFarmCacheCannotExposeDeletedOrForeignPonds() {
        @SuppressWarnings("unchecked")
        var hash = (org.springframework.data.redis.core.HashOperations<String, Object, Object>) org.mockito.Mockito.mock(org.springframework.data.redis.core.HashOperations.class);
        org.mockito.Mockito.when(redis.opsForHash()).thenReturn(hash);
        org.mockito.Mockito.when(hash.entries("iot:latest:farm:10")).thenReturn(Map.of(
                "101", "{\"pondId\":101}", "102", "{\"pondId\":102}", "201", "{\"pondId\":201}"));
        jdbc.update("UPDATE t_pond SET is_deleted=1 WHERE id=102");
        assertThat(sensorData.getLatestByFarmId(10L)).extracting(com.Zx1nggg.FAMS.modules.iot.vo.IotSensorDataVO::getPondId).containsExactly(101L);
        assertThatThrownBy(() -> sensorData.getLatestByFarmId(20L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> sensorData.getLatestByPondId(201L)).isInstanceOf(BusinessException.class);
    }
    @Test void settledPondLedgerCannotBeDeletedAndPatrolCannotOrphanChildren() {
        PatrolLog patrol = new PatrolLog(); patrol.setPondId(101L); patrol.setBatchNo("TEST-BATCH"); patrols.save(patrol);
        BatchGrowthLog observation = new BatchGrowthLog(); observation.setPondId(101L); observation.setBatchNo("TEST-BATCH"); observation.setPatrolLogId(patrol.getId()); growth.save(observation);
        PondFeedLog feed = new PondFeedLog(); feed.setPondId(101L); feed.setPatrolLogId(patrol.getId()); feeds.save(feed);
        assertThatThrownBy(() -> patrols.batchDelete(List.of(patrol.getId()))).isInstanceOf(BusinessException.class);
        harvest.create(dto(101L));
        assertThatThrownBy(() -> growth.batchDelete(List.of(observation.getId()))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> feeds.batchDelete(List.of(feed.getId()))).isInstanceOf(BusinessException.class);
        assertThat(growth.getById(observation.getId())).isNotNull(); assertThat(feeds.getById(feed.getId())).isNotNull();
    }
    private void createTable(Class<?> type) {
        List<String> columns = new ArrayList<>();
        for (var field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            TableField annotation = field.getAnnotation(TableField.class);
            if (annotation != null && !annotation.exist()) continue;
            String name = annotation != null && !annotation.value().isBlank() ? annotation.value()
                    : field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            String sqlType = field.getType() == Long.class ? "BIGINT" : field.getType() == Integer.class || field.getType() == Byte.class ? "INT"
                    : field.getType() == BigDecimal.class ? "DECIMAL(18,4)" : field.getType() == LocalDate.class ? "DATE"
                    : field.getType() == java.time.LocalDateTime.class ? "TIMESTAMP" : "VARCHAR(2000)";
            columns.add(name + " " + sqlType + (field.isAnnotationPresent(TableId.class) ? " AUTO_INCREMENT PRIMARY KEY" : "")
                    + (name.equals("is_deleted") || name.equals("auth_version") ? " DEFAULT 0 NOT NULL" : ""));
        }
        jdbc.execute("CREATE TABLE " + type.getAnnotation(TableName.class).value() + " (" + String.join(",", columns) + ")");
    }
}
