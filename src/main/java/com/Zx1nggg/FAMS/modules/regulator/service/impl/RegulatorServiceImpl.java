package com.Zx1nggg.FAMS.modules.regulator.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.*;
import com.Zx1nggg.FAMS.modules.base.mapper.*;
import com.Zx1nggg.FAMS.modules.iot.entity.IotSensorData;
import com.Zx1nggg.FAMS.modules.iot.mapper.IotSensorDataMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.PatrolLog;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.BatchGrowthLogMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.HarvestRecordMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.PatrolLogMapper;
import com.Zx1nggg.FAMS.modules.log.entity.AlarmRecord;
import com.Zx1nggg.FAMS.modules.log.entity.PondFeedLog;
import com.Zx1nggg.FAMS.modules.log.mapper.AlarmRecordMapper;
import com.Zx1nggg.FAMS.modules.log.mapper.PondFeedLogMapper;
import com.Zx1nggg.FAMS.modules.log.service.IAlarmRecordService;
import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.Zx1nggg.FAMS.modules.regulator.service.IRegulatorService;
import com.Zx1nggg.FAMS.modules.regulator.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegulatorServiceImpl implements IRegulatorService {

    @Autowired private FarmMapper farmMapper;
    @Autowired private PondMapper pondMapper;
    @Autowired private StockingMapper stockingMapper;
    @Autowired private PurchaseBatchMapper purchaseBatchMapper;
    @Autowired private SeedlingDictMapper seedlingDictMapper;
    @Autowired private BatchGrowthLogMapper batchGrowthLogMapper;
    @Autowired private HarvestRecordMapper harvestRecordMapper;
    @Autowired private AlarmRecordMapper alarmRecordMapper;
    @Autowired private IotSensorDataMapper iotSensorDataMapper;
    @Autowired private IAlarmRecordService alarmRecordService;
    @Autowired private SupplierMapper supplierMapper;
    @Autowired private PatrolLogMapper patrolLogMapper;
    @Autowired private PondFeedLogMapper pondFeedLogMapper;

    @Override
    public DashboardStatsVO getDashboardStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setTotalFarms(farmMapper.selectCount(new LambdaQueryWrapper<Farm>().eq(Farm::getIsDeleted, 0)));

        long totalStocked = stockingMapper.selectList(new LambdaQueryWrapper<Stocking>().eq(Stocking::getIsDeleted, 0))
                .stream()
                .mapToLong(item -> item.getStockedQty() == null ? 0L : item.getStockedQty())
                .sum();
        stats.setTotalLiveStock(totalStocked / 10000L);

        YearMonth month = YearMonth.now();
        stats.setMonthlyCertificates(purchaseBatchMapper.selectCount(new LambdaQueryWrapper<PurchaseBatch>()
                .ge(PurchaseBatch::getBatchStatus, (byte) 1)
                .ge(PurchaseBatch::getPurchaseDate, month.atDay(1))
                .le(PurchaseBatch::getPurchaseDate, month.atEndOfMonth())));

        List<AlarmRecord> active = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>().in(AlarmRecord::getStatus, 0, 1, 2));
        stats.setUnhandledAlertFarms(active.stream().map(AlarmRecord::getFarmId).filter(Objects::nonNull).distinct().count());
        return stats;
    }

    @Override
    public List<DashboardAlertVO> getDashboardAlerts(Integer limit) {
        int size = limit == null || limit <= 0 ? 5 : limit;
        List<AlarmRecord> alarms = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
                .in(AlarmRecord::getStatus, 0, 1, 2)
                .orderByDesc(AlarmRecord::getSeverity)
                .orderByDesc(AlarmRecord::getLastOccurredAt)
                .last("LIMIT " + size));

        return alarms.stream().map(alarm -> {
            DashboardAlertVO vo = new DashboardAlertVO();
            vo.setAlarmId(alarm.getId());
            vo.setFarmId(alarm.getFarmId());
            vo.setPondId(alarm.getPondId());
            vo.setAlarmCode(alarm.getAlarmCode());
            vo.setTitle(alarm.getTitle());
            vo.setMessage(alarm.getMessage());
            vo.setSeverity(alarm.getSeverity());
            vo.setStatus(alarm.getStatus());
            vo.setOccurrenceCount(alarm.getOccurrenceCount());
            vo.setLastOccurredAt(alarm.getLastOccurredAt());
            Farm farm = alarm.getFarmId() == null ? null : farmMapper.selectById(alarm.getFarmId());
            vo.setFarmName(farm == null ? "未知养殖场" : farm.getFarmName());
            return vo;
        }).toList();
    }

    @Override
    public List<DashboardWatchlistVO> getDashboardWatchlist(Integer limit) {
        int size = limit == null || limit <= 0 ? 10 : limit;
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Farm> farms = farmMapper.selectList(new LambdaQueryWrapper<Farm>().eq(Farm::getIsDeleted, 0));
        List<DashboardWatchlistVO> result = new ArrayList<>();

        for (Farm farm : farms) {
            Long alarmCount = alarmRecordMapper.selectCount(new LambdaQueryWrapper<AlarmRecord>()
                    .eq(AlarmRecord::getFarmId, farm.getId())
                    .ge(AlarmRecord::getLastOccurredAt, since));
            int abnormalDeaths = countRecentAbnormalDeaths(farm.getId(), since.toLocalDate());
            if (alarmCount <= 0 && abnormalDeaths <= 0) continue;

            DashboardWatchlistVO vo = new DashboardWatchlistVO();
            vo.setFarmId(farm.getId());
            vo.setFarmName(farm.getFarmName());
            vo.setAlarmCount(alarmCount.intValue());
            if (abnormalDeaths > 100) {
                vo.setRiskType("死亡率异常");
                vo.setRiskDescription("近30天累计异常死亡 " + abnormalDeaths + " 尾，建议重点核查养殖环境与用药记录");
                vo.setRiskMetric(abnormalDeaths + "尾");
            } else if (alarmCount >= 5) {
                vo.setRiskType("环境异常");
                vo.setRiskDescription("近30天累计触发 " + alarmCount + " 次告警，水质或生产环境波动频繁");
                vo.setRiskMetric(alarmCount + "次");
            } else {
                vo.setRiskType("违规操作");
                vo.setRiskDescription("近30天存在未闭环风险事件，建议监管员跟进处理记录");
                vo.setRiskMetric(alarmCount + "次");
            }
            result.add(vo);
        }

        result.sort((a, b) -> Integer.compare(b.getAlarmCount(), a.getAlarmCount()));
        return result.stream().limit(size).toList();
    }

    @Override
    public List<FarmGeoVO> getFarmsGeo() {
        List<Farm> farms = farmMapper.selectList(new LambdaQueryWrapper<Farm>().eq(Farm::getIsDeleted, 0));
        List<FarmGeoVO> result = new ArrayList<>();
        for (Farm farm : farms) {
            List<Pond> ponds = pondMapper.selectList(new LambdaQueryWrapper<Pond>()
                    .eq(Pond::getFarmId, farm.getId())
                    .eq(Pond::getIsDeleted, 0));
            List<AlarmRecord> activeAlarms = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
                    .eq(AlarmRecord::getFarmId, farm.getId())
                    .in(AlarmRecord::getStatus, 0, 1, 2));

            FarmGeoVO vo = new FarmGeoVO();
            vo.setFarmId(farm.getId());
            vo.setFarmName(farm.getFarmName());
            vo.setLongitude(farm.getLongitude());
            vo.setLatitude(farm.getLatitude());
            vo.setAddress(farm.getAddress());
            vo.setProvince(extractProvince(farm.getAddress(), farm.getFarmName()));
            vo.setPondCount(ponds.size());
            vo.setActiveAlarmCount(activeAlarms.size());
            boolean critical = activeAlarms.stream().anyMatch(item -> Objects.equals(item.getSeverity(), (byte) 3));
            vo.setAlertStatus(critical ? "critical" : activeAlarms.isEmpty() ? "normal" : "warning");
            long stock = currentStockCount(farm.getId(), ponds);
            vo.setStockCount(stock);
            vo.setStockAmount(stock / 10000L);
            vo.setMainSpecies(mainSpecies(farm.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public TraceChainVO quickTrace(String keyword) {
        if (keyword == null || keyword.isBlank()) throw new BusinessException(400, "keyword is required");
        PurchaseBatch batch = purchaseBatchMapper.selectOne(new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, keyword));
        if (batch == null) {
            HarvestRecord harvest = harvestRecordMapper.selectOne(new LambdaQueryWrapper<HarvestRecord>()
                    .like(HarvestRecord::getTraceQrCodeUrl, keyword)
                    .eq(HarvestRecord::getIsDeleted, 0)
                    .last("LIMIT 1"));
            if (harvest != null) {
                batch = purchaseBatchMapper.selectOne(new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, harvest.getBatchNo()));
            }
        }
        if (batch == null) {
            batch = purchaseBatchMapper.selectOne(new LambdaQueryWrapper<PurchaseBatch>().like(PurchaseBatch::getBatchNo, keyword).last("LIMIT 1"));
        }
        if (batch == null) throw new BusinessException(404, "trace batch not found");
        return buildTraceChain(batch);
    }

    @Override
    public TraceChainVO getTraceDetail(String batchNo) {
        if (batchNo == null || batchNo.isBlank()) throw new BusinessException(400, "batchNo is required");
        PurchaseBatch batch = purchaseBatchMapper.selectOne(new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, batchNo));
        if (batch == null) throw new BusinessException(404, "trace batch not found");
        return buildTraceChain(batch);
    }

    @Override
    public Page<TraceBatchVO> listTraceBatches(Integer pageNum, Integer pageSize, Long farmId, Byte batchStatus, String keyword) {
        int current = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        int size = pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 100);

        LambdaQueryWrapper<PurchaseBatch> wrapper = new LambdaQueryWrapper<>();
        if (farmId != null) wrapper.eq(PurchaseBatch::getFarmId, farmId);
        if (batchStatus != null) wrapper.eq(PurchaseBatch::getBatchStatus, batchStatus);
        if (keyword != null && !keyword.isBlank()) wrapper.like(PurchaseBatch::getBatchNo, keyword.trim());
        wrapper.orderByDesc(PurchaseBatch::getPurchaseDate).orderByDesc(PurchaseBatch::getId);

        Page<PurchaseBatch> page = purchaseBatchMapper.selectPage(new Page<>(current, size), wrapper);
        Page<TraceBatchVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toTraceBatchVO).toList());
        return voPage;
    }

    @Override
    public AlertStatsVO getAlertStats() {
        List<AlarmRecord> alarms = alarmRecordMapper.selectList(null);
        LocalDate today = LocalDate.now();
        AlertStatsVO stats = new AlertStatsVO();
        stats.setTotalCount((long) alarms.size());
        stats.setActiveCount(alarms.stream().filter(a -> isActiveStatus(a.getStatus())).count());
        stats.setPendingCount(alarms.stream().filter(a -> Objects.equals(a.getStatus(), (byte) 0)).count());
        stats.setProcessingCount(alarms.stream().filter(a -> Objects.equals(a.getStatus(), (byte) 2)).count());
        stats.setResolvedCount(alarms.stream().filter(a -> Objects.equals(a.getStatus(), (byte) 3)).count());
        stats.setCriticalCount(alarms.stream().filter(a -> Objects.equals(a.getSeverity(), (byte) 3)).count());
        stats.setTodayNewCount(alarms.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().toLocalDate().equals(today))
                .count());
        stats.setByType(alarms.stream().collect(Collectors.groupingBy(
                a -> a.getSourceType() == null ? "UNKNOWN" : a.getSourceType(), LinkedHashMap::new, Collectors.counting())));
        stats.setByLevel(alarms.stream().collect(Collectors.groupingBy(
                a -> a.getSeverity() == null ? "0" : String.valueOf(a.getSeverity()), LinkedHashMap::new, Collectors.counting())));
        return stats;
    }

    @Override
    public Page<AlarmRecordVO> listAlerts(Integer pageNum, Integer pageSize, Long farmId, Byte severity,
                                          Byte status, String sourceType, LocalDate startDate, LocalDate endDate) {
        int current = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        if (farmId != null) wrapper.eq(AlarmRecord::getFarmId, farmId);
        if (severity != null) wrapper.eq(AlarmRecord::getSeverity, severity);
        if (status != null) wrapper.eq(AlarmRecord::getStatus, status);
        if (sourceType != null && !sourceType.isBlank()) wrapper.eq(AlarmRecord::getSourceType, sourceType);
        if (startDate != null) wrapper.ge(AlarmRecord::getCreatedAt, startDate.atStartOfDay());
        if (endDate != null) wrapper.le(AlarmRecord::getCreatedAt, endDate.plusDays(1).atStartOfDay().minusNanos(1));
        wrapper.orderByAsc(AlarmRecord::getStatus)
                .orderByDesc(AlarmRecord::getSeverity)
                .orderByDesc(AlarmRecord::getLastOccurredAt);

        Page<AlarmRecord> page = alarmRecordMapper.selectPage(new Page<>(current, size), wrapper);
        Page<AlarmRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toAlarmRecordVO).toList());
        return voPage;
    }

    @Override
    public List<AlertTrendVO> getAlertTrend(Integer days) {
        int range = days == null || days <= 0 ? 30 : Math.min(days, 180);
        LocalDate start = LocalDate.now().minusDays(range - 1L);
        List<AlarmRecord> alarms = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
                        .ge(AlarmRecord::getCreatedAt, start.atStartOfDay()))
                .stream()
                .filter(a -> a.getCreatedAt() != null)
                .toList();
        Map<LocalDate, List<AlarmRecord>> byDate = alarms.stream().collect(Collectors.groupingBy(a -> a.getCreatedAt().toLocalDate()));

        List<AlertTrendVO> result = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            LocalDate date = start.plusDays(i);
            List<AlarmRecord> dayAlarms = byDate.getOrDefault(date, List.of());
            AlertTrendVO vo = new AlertTrendVO();
            vo.setDate(date.toString());
            vo.setTotalCount(dayAlarms.size());
            vo.setCriticalCount((int) dayAlarms.stream().filter(a -> Objects.equals(a.getSeverity(), (byte) 3)).count());
            vo.setWarningCount((int) dayAlarms.stream().filter(a -> Objects.equals(a.getSeverity(), (byte) 2)).count());
            vo.setInfoCount((int) dayAlarms.stream().filter(a -> Objects.equals(a.getSeverity(), (byte) 1)).count());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<IotRealtimeAlertVO> getIotRealtimeAlerts() {
        List<Pond> ponds = pondMapper.selectList(new LambdaQueryWrapper<Pond>().eq(Pond::getIsDeleted, 0));
        List<IotRealtimeAlertVO> result = new ArrayList<>();
        for (Pond pond : ponds) {
            IotSensorData latest = iotSensorDataMapper.selectOne(new LambdaQueryWrapper<IotSensorData>()
                    .eq(IotSensorData::getPondId, pond.getId())
                    .orderByDesc(IotSensorData::getCollectTime)
                    .last("LIMIT 1"));
            if (latest == null) continue;
            Farm farm = pond.getFarmId() == null ? null : farmMapper.selectById(pond.getFarmId());
            addIotAlertIfNeeded(result, pond, farm, latest, "waterTemp", latest.getWaterTemp(), null, bd("35.0"));
            addIotAlertIfNeeded(result, pond, farm, latest, "doLevel", latest.getDissolvedOxygen(), bd("3.5"), null);
            addIotAlertIfNeeded(result, pond, farm, latest, "phLevel", latest.getPhValue(), bd("6.5"), bd("9.0"));
        }
        result.sort((a, b) -> {
            if (a.getDataTime() == null && b.getDataTime() == null) return 0;
            if (a.getDataTime() == null) return 1;
            if (b.getDataTime() == null) return -1;
            return b.getDataTime().compareTo(a.getDataTime());
        });
        return result;
    }

    @Override
    public void handleAlert(Long id, Byte status, String remark) {
        if (status == null) throw new BusinessException(400, "status is required");
        switch (status) {
            case 0 -> alarmRecordService.reopen(id, remark);
            case 1 -> alarmRecordService.acknowledge(id, remark);
            case 2 -> alarmRecordService.startProcessing(id, remark);
            case 3 -> alarmRecordService.resolve(id, remark);
            case 4 -> alarmRecordService.close(id, remark);
            default -> throw new BusinessException(400, "unsupported status: " + status);
        }
    }

    @Override
    public List<SurvivalRateVO> getSurvivalRate(LocalDate startDate, LocalDate endDate, Long farmId, Long seedlingId, String groupBy) {
        String dim = groupBy == null || groupBy.isBlank() ? "batch" : groupBy.trim().toLowerCase();
        if (!Set.of("batch", "farm", "seedling").contains(dim)) {
            throw new BusinessException(400, "groupBy only supports batch/farm/seedling");
        }

        Map<String, SurvivalAccumulator> grouped = new LinkedHashMap<>();
        for (BatchSurvivalSnapshot snapshot : buildSurvivalSnapshots(startDate, endDate, farmId, seedlingId)) {
            String key = switch (dim) {
                case "farm" -> String.valueOf(snapshot.farmId);
                case "seedling" -> String.valueOf(snapshot.seedlingId);
                default -> snapshot.batchNo;
            };
            String label = switch (dim) {
                case "farm" -> blankToDash(snapshot.farmName);
                case "seedling" -> blankToDash(snapshot.seedlingName);
                default -> snapshot.batchNo;
            };
            grouped.computeIfAbsent(key, k -> new SurvivalAccumulator(k, label)).add(snapshot);
        }

        return grouped.values().stream()
                .map(SurvivalAccumulator::toVO)
                .sorted(Comparator.comparing(SurvivalRateVO::getSurvivalRate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<SurvivalTrendVO> getSurvivalTrend(LocalDate startDate, LocalDate endDate, Long farmId, Long seedlingId) {
        Map<String, List<BatchSurvivalSnapshot>> byMonth = buildSurvivalSnapshots(startDate, endDate, farmId, seedlingId)
                .stream()
                .collect(Collectors.groupingBy(item -> item.analysisDate == null ? "未知月份" : YearMonth.from(item.analysisDate).toString(),
                        LinkedHashMap::new, Collectors.toList()));
        return byMonth.entrySet().stream().map(entry -> {
            List<BigDecimal> rates = entry.getValue().stream()
                    .map(item -> item.survivalRate)
                    .filter(Objects::nonNull)
                    .toList();
            SurvivalTrendVO vo = new SurvivalTrendVO();
            vo.setMonth(entry.getKey());
            vo.setBatchCount(entry.getValue().size());
            vo.setAvgSurvivalRate(avg(rates));
            vo.setMaxRate(rates.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            vo.setMinRate(rates.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            return vo;
        }).sorted(Comparator.comparing(SurvivalTrendVO::getMonth)).toList();
    }

    @Override
    public ProductionStatsVO getProductionStats(LocalDate startDate, LocalDate endDate, Long farmId, Long seedlingId) {
        List<ProductionSnapshot> snapshots = buildProductionSnapshots(startDate, endDate, farmId, seedlingId);
        ProductionStatsVO vo = new ProductionStatsVO();
        vo.setTotalProductionKg(sum(snapshots.stream().map(item -> item.totalProductionKg).toList()));
        vo.setTotalRevenue(sum(snapshots.stream().map(item -> item.totalRevenue).toList()));
        vo.setTotalCost(sum(snapshots.stream().map(item -> item.totalCost).toList()));
        vo.setNetProfit(sum(snapshots.stream().map(item -> item.netProfit).toList()));
        vo.setHarvestCount(snapshots.size());
        vo.setParticipatingFarmCount((int) snapshots.stream().map(item -> item.farmId).filter(Objects::nonNull).distinct().count());
        vo.setAvgUnitPrice(weightedAvgUnitPrice(snapshots));
        return vo;
    }

    @Override
    public List<ProductionRankingVO> getProductionRanking(LocalDate startDate, LocalDate endDate, Long farmId, Long seedlingId, Integer limit) {
        Map<Long, List<ProductionSnapshot>> grouped = buildProductionSnapshots(startDate, endDate, farmId, seedlingId)
                .stream()
                .filter(item -> item.farmId != null)
                .collect(Collectors.groupingBy(item -> item.farmId, LinkedHashMap::new, Collectors.toList()));
        int max = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);
        List<ProductionRankingVO> rows = grouped.entrySet().stream().map(entry -> {
            List<ProductionSnapshot> items = entry.getValue();
            ProductionRankingVO vo = new ProductionRankingVO();
            vo.setFarmId(entry.getKey());
            vo.setFarmName(items.stream().map(item -> item.farmName).filter(Objects::nonNull).findFirst().orElse("未知养殖场"));
            vo.setTotalProductionKg(sum(items.stream().map(item -> item.totalProductionKg).toList()));
            vo.setTotalRevenue(sum(items.stream().map(item -> item.totalRevenue).toList()));
            vo.setNetProfit(sum(items.stream().map(item -> item.netProfit).toList()));
            vo.setHarvestCount(items.size());
            return vo;
        }).sorted(Comparator.comparing(ProductionRankingVO::getTotalProductionKg, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(max)
                .toList();
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRanking(i + 1);
        }
        return rows;
    }

    @Override
    public byte[] exportAnalysis(String type, LocalDate startDate, LocalDate endDate, Long farmId, Long seedlingId) {
        String exportType = type == null || type.isBlank() ? "all" : type.trim().toLowerCase();
        if (!Set.of("all", "survival", "production").contains(exportType)) {
            throw new BusinessException(400, "type only supports all/survival/production");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            if ("all".equals(exportType) || "survival".equals(exportType)) {
                XSSFSheet sheet = workbook.createSheet("成活率统计");
                writeRow(sheet, 0, headerStyle, "维度", "养殖场", "苗种", "投放尾数", "估算出塘尾数", "死亡尾数", "成活率(%)", "收获重量(kg)", "批次数");
                List<SurvivalRateVO> rows = getSurvivalRate(startDate, endDate, farmId, seedlingId, "batch");
                for (int i = 0; i < rows.size(); i++) {
                    SurvivalRateVO row = rows.get(i);
                    writeRow(sheet, i + 1, null,
                            row.getDimLabel(), row.getFarmName(), row.getSeedlingName(), row.getStockedQty(),
                            row.getEstimatedHarvestQty(), row.getDeathQty(), row.getSurvivalRate(),
                            row.getTotalHarvestWeightKg(), row.getBatchCount());
                }
                autosize(sheet, 9);
            }
            if ("all".equals(exportType) || "production".equals(exportType)) {
                XSSFSheet sheet = workbook.createSheet("产量统计");
                writeRow(sheet, 0, headerStyle, "排名", "养殖场", "产量(kg)", "产值(元)", "净利润(元)", "出塘批次");
                List<ProductionRankingVO> rows = getProductionRanking(startDate, endDate, farmId, seedlingId, 100);
                for (int i = 0; i < rows.size(); i++) {
                    ProductionRankingVO row = rows.get(i);
                    writeRow(sheet, i + 1, null, row.getRanking(), row.getFarmName(), row.getTotalProductionKg(),
                            row.getTotalRevenue(), row.getNetProfit(), row.getHarvestCount());
                }
                autosize(sheet, 6);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "导出报表失败");
        }
    }

    private TraceChainVO buildTraceChain(PurchaseBatch batch) {
        TraceChainVO chain = new TraceChainVO();
        chain.setBatchNo(batch.getBatchNo());
        chain.setSeedlingName(seedlingName(batch.getSeedlingId()));
        List<TraceNodeVO> nodes = new ArrayList<>();

        if (batch.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(batch.getSupplierId());
            if (supplier != null) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("供应商名称", supplier.getSupplierName());
                detail.put("联系人", supplier.getContactPerson());
                detail.put("联系电话", supplier.getContactPhone());
                detail.put("许可证号", supplier.getQualificationCode());
                nodes.add(traceNode("supplier", "苗种来源", supplier.getCreateTime(), detail));
            }
        }

        Map<String, Object> purchase = new LinkedHashMap<>();
        purchase.put("批次号", batch.getBatchNo());
        purchase.put("采购日期", batch.getPurchaseDate());
        purchase.put("采购件数", safe(batch.getUnitQty()) + " " + safe(batch.getPurchaseUnit()));
        purchase.put("每件密度", safe(batch.getDensityPerUnit()) + " 尾/" + safe(batch.getPurchaseUnit()));
        purchase.put("预计总尾数", batch.getEstimatedTotalQty());
        purchase.put("单价", batch.getUnitPrice() == null ? "-" : batch.getUnitPrice() + " 元/" + safe(batch.getPurchaseUnit()));
        purchase.put("总金额", batch.getTotalAmount() == null ? "-" : batch.getTotalAmount() + " 元");
        purchase.put("检疫证号", batch.getQuarantineCertNo());
        purchase.put("批次状态", batchStatusLabel(batch.getBatchStatus()));
        nodes.add(traceNode("purchase", "苗种采购", batch.getPurchaseDate() == null ? null : batch.getPurchaseDate().atStartOfDay(), purchase));

        List<Stocking> stockings = stockingMapper.selectList(new LambdaQueryWrapper<Stocking>()
                .eq(Stocking::getBatchId, batch.getId())
                .eq(Stocking::getIsDeleted, 0));
        for (Stocking stocking : stockings) {
            Map<String, Object> detail = new LinkedHashMap<>();
            Pond pond = stocking.getPondId() == null ? null : pondMapper.selectById(stocking.getPondId());
            detail.put("投放池塘", pond == null ? "ID:" + stocking.getPondId() : pond.getPondName());
            detail.put("投放件数", safe(stocking.getStockedUnits()) + " 件");
            detail.put("投放尾数", safe(stocking.getStockedQty()) + " 尾");
            detail.put("投放总重", stocking.getStockedWeight() == null ? "-" : stocking.getStockedWeight() + " kg");
            detail.put("投放日期", stocking.getStockingDate());
            detail.put("备注", blankToDash(stocking.getRemark()));
            nodes.add(traceNode("stocking", "投放登记", stocking.getStockingDate() == null ? stocking.getCreateTime() : stocking.getStockingDate().atStartOfDay(), detail));
        }

        List<BatchGrowthLog> growthLogs = batchGrowthLogMapper.selectList(new LambdaQueryWrapper<BatchGrowthLog>()
                .eq(BatchGrowthLog::getBatchNo, batch.getBatchNo())
                .orderByAsc(BatchGrowthLog::getLogDate));
        for (BatchGrowthLog growth : growthLogs) {
            Map<String, Object> detail = new LinkedHashMap<>();
            Pond pond = growth.getPondId() == null ? null : pondMapper.selectById(growth.getPondId());
            detail.put("池塘", pond == null ? "-" : pond.getPondName());
            detail.put("抽测日期", growth.getLogDate());
            detail.put("均长", growth.getAvgLength() == null ? "-" : growth.getAvgLength() + " cm");
            detail.put("均重", growth.getAvgWeight() == null ? "-" : growth.getAvgWeight() + " g");
            detail.put("日常死亡", safe(growth.getRoutineDeathCount()) + " 尾");
            detail.put("异常死亡", safe(growth.getAbnormalDeathCount()) + " 尾");
            if (growth.getAbnormalReason() != null && !growth.getAbnormalReason().isBlank()) {
                detail.put("异常原因", growth.getAbnormalReason());
            }
            nodes.add(traceNode("growth", "成长抽测记录", growth.getLogDate() == null ? null : growth.getLogDate().atStartOfDay(), detail));
        }

        List<PatrolLog> patrolLogs = patrolLogMapper.selectList(new LambdaQueryWrapper<PatrolLog>()
                .eq(PatrolLog::getBatchNo, batch.getBatchNo())
                .eq(PatrolLog::getIsDeleted, 0)
                .orderByAsc(PatrolLog::getPatrolTime));
        for (PatrolLog patrol : patrolLogs) {
            Map<String, Object> detail = new LinkedHashMap<>();
            Pond pond = patrol.getPondId() == null ? null : pondMapper.selectById(patrol.getPondId());
            detail.put("池塘", pond == null ? "-" : pond.getPondName());
            detail.put("巡塘时间", patrol.getPatrolTime());
            detail.put("天气", blankToDash(patrol.getWeather()));
            detail.put("水温", patrol.getWaterTemp() == null ? "-" : patrol.getWaterTemp() + " °C");
            detail.put("水色", blankToDash(patrol.getWaterColor()));
            detail.put("备注", blankToDash(patrol.getRemark()));
            nodes.add(traceNode("patrol", "巡塘记录", patrol.getPatrolTime(), detail));
        }

        Set<Long> patrolIds = patrolLogs.stream()
                .map(PatrolLog::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!patrolIds.isEmpty()) {
            List<PondFeedLog> feedLogs = pondFeedLogMapper.selectList(new LambdaQueryWrapper<PondFeedLog>()
                    .in(PondFeedLog::getPatrolLogId, patrolIds)
                    .orderByAsc(PondFeedLog::getLogDate));
            for (PondFeedLog feed : feedLogs) {
                Map<String, Object> detail = new LinkedHashMap<>();
                Pond pond = feed.getPondId() == null ? null : pondMapper.selectById(feed.getPondId());
                detail.put("池塘", pond == null ? "-" : pond.getPondName());
                detail.put("操作日期", feed.getLogDate());
                detail.put("饲料品牌", blankToDash(feed.getFeedBrand()));
                detail.put("投喂量", feed.getFeedAmount() == null ? "-" : feed.getFeedAmount() + " kg");
                detail.put("饲料单价", feed.getFeedUnitPrice() == null ? "-" : feed.getFeedUnitPrice() + " 元/kg");
                detail.put("换水状态", blankToDash(feed.getWaterChangeStatus()));
                detail.put("药品名称", blankToDash(feed.getMedicineName()));
                detail.put("用药量", feed.getMedicineDosage() == null ? "-" : feed.getMedicineDosage() + " " + safe(feed.getMedicineUnit()));
                detail.put("药费", feed.getMedicineAmount() == null ? "-" : feed.getMedicineAmount() + " 元");
                nodes.add(traceNode("feed", "投喂与环境作业", feed.getLogDate() == null ? null : feed.getLogDate().atStartOfDay(), detail));
            }
        }

        HarvestRecord harvest = harvestRecordMapper.selectOne(new LambdaQueryWrapper<HarvestRecord>()
                .eq(HarvestRecord::getBatchNo, batch.getBatchNo())
                .eq(HarvestRecord::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (harvest != null) {
            Map<String, Object> detail = new LinkedHashMap<>();
            Pond pond = harvest.getPondId() == null ? null : pondMapper.selectById(harvest.getPondId());
            detail.put("出塘日期", harvest.getHarvestDate());
            detail.put("出塘池塘", pond == null ? "-" : pond.getPondName());
            detail.put("实际总重", harvest.getActualTotalWeightKg() == null ? "-" : harvest.getActualTotalWeightKg() + " kg");
            detail.put("实际均重", harvest.getActualAvgWeightG() == null ? "-" : harvest.getActualAvgWeightG() + " g/尾");
            detail.put("收购方", blankToDash(harvest.getBuyerName()));
            detail.put("单价", harvest.getUnitPrice() == null ? "-" : harvest.getUnitPrice() + " 元/kg");
            detail.put("总收入", harvest.getTotalRevenue() == null ? "-" : harvest.getTotalRevenue() + " 元");
            detail.put("总成本", harvest.getTotalCost() == null ? "-" : harvest.getTotalCost() + " 元");
            detail.put("净利润", harvest.getNetProfit() == null ? "-" : harvest.getNetProfit() + " 元");
            detail.put("结算状态", harvest.getSettlementStatus() != null && harvest.getSettlementStatus() == 1 ? "已结算" : "未结算");
            nodes.add(traceNode("harvest", "出塘结算", harvest.getHarvestDate() == null ? harvest.getCreateTime() : harvest.getHarvestDate().atStartOfDay(), detail));
        }

        nodes.sort(Comparator.comparing(TraceNodeVO::getNodeTime, Comparator.nullsLast(Comparator.naturalOrder())));
        chain.setNodes(nodes);
        return chain;
    }
    private TraceNodeVO traceNode(String type, String name, LocalDateTime time, Map<String, Object> detail) {
        TraceNodeVO node = new TraceNodeVO();
        node.setNodeType(type);
        node.setNodeName(name);
        node.setNodeTime(time);
        node.setDetail(detail);
        return node;
    }

    private TraceBatchVO toTraceBatchVO(PurchaseBatch batch) {
        TraceBatchVO vo = new TraceBatchVO();
        vo.setId(batch.getId());
        vo.setFarmId(batch.getFarmId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setSeedlingName(seedlingName(batch.getSeedlingId()));
        vo.setEstimatedTotalQty(batch.getEstimatedTotalQty());
        vo.setBatchStatus(batch.getBatchStatus());
        vo.setPurchaseDate(batch.getPurchaseDate());

        Farm farm = batch.getFarmId() == null ? null : farmMapper.selectById(batch.getFarmId());
        Supplier supplier = batch.getSupplierId() == null ? null : supplierMapper.selectById(batch.getSupplierId());
        vo.setFarmName(farm == null ? null : farm.getFarmName());
        vo.setSupplierName(supplier == null ? null : supplier.getSupplierName());
        return vo;
    }

    private int countRecentAbnormalDeaths(Long farmId, LocalDate since) {
        List<Long> pondIds = pondMapper.selectList(new LambdaQueryWrapper<Pond>()
                        .eq(Pond::getFarmId, farmId)
                        .eq(Pond::getIsDeleted, 0))
                .stream()
                .map(Pond::getId)
                .filter(Objects::nonNull)
                .toList();
        if (pondIds.isEmpty()) return 0;
        return batchGrowthLogMapper.selectList(new LambdaQueryWrapper<BatchGrowthLog>()
                        .in(BatchGrowthLog::getPondId, pondIds)
                        .ge(BatchGrowthLog::getLogDate, since)
                        .gt(BatchGrowthLog::getAbnormalDeathCount, 0))
                .stream()
                .mapToInt(item -> item.getAbnormalDeathCount() == null ? 0 : item.getAbnormalDeathCount())
                .sum();
    }

    private long currentStockCount(Long farmId, List<Pond> ponds) {
        Set<Long> pondIds = ponds.stream().map(Pond::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (pondIds.isEmpty()) return 0L;
        List<PurchaseBatch> activeBatches = purchaseBatchMapper.selectList(new LambdaQueryWrapper<PurchaseBatch>()
                .eq(PurchaseBatch::getFarmId, farmId)
                .eq(PurchaseBatch::getBatchStatus, (byte) 2));
        Set<Long> activeBatchIds = activeBatches.stream().map(PurchaseBatch::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (activeBatchIds.isEmpty()) return 0L;
        return stockingMapper.selectList(new LambdaQueryWrapper<Stocking>()
                        .in(Stocking::getPondId, pondIds)
                        .in(Stocking::getBatchId, activeBatchIds)
                        .eq(Stocking::getIsDeleted, 0))
                .stream()
                .mapToLong(item -> item.getStockedQty() == null ? 0L : item.getStockedQty())
                .sum();
    }

    private String mainSpecies(Long farmId) {
        return purchaseBatchMapper.selectList(new LambdaQueryWrapper<PurchaseBatch>()
                        .eq(PurchaseBatch::getFarmId, farmId)
                        .eq(PurchaseBatch::getBatchStatus, (byte) 2))
                .stream()
                .map(item -> seedlingName(item.getSeedlingId()))
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .collect(Collectors.joining("、"));
    }

    private String seedlingName(Long seedlingId) {
        if (seedlingId == null) return null;
        SeedlingDict seedling = seedlingDictMapper.selectById(seedlingId);
        return seedling == null ? null : seedling.getCategoryName();
    }

    private AlarmRecordVO toAlarmRecordVO(AlarmRecord record) {
        AlarmRecordVO vo = new AlarmRecordVO();
        vo.setId(record.getId());
        vo.setFarmId(record.getFarmId());
        vo.setPondId(record.getPondId());
        vo.setRuleId(record.getRuleId());
        vo.setAlarmCode(record.getAlarmCode());
        vo.setTitle(record.getTitle());
        vo.setMessage(record.getMessage());
        vo.setSourceType(record.getSourceType());
        vo.setSourceId(record.getSourceId());
        vo.setSeverity(record.getSeverity());
        vo.setStatus(record.getStatus());
        vo.setMetricCode(record.getMetricCode());
        vo.setTriggerValue(record.getTriggerValue());
        vo.setThresholdOperator(record.getThresholdOperator());
        vo.setThresholdValue(record.getThresholdValue());
        vo.setThresholdValueHigh(record.getThresholdValueHigh());
        vo.setMetricUnit(record.getMetricUnit());
        vo.setOccurrenceCount(record.getOccurrenceCount());
        vo.setFirstOccurredAt(record.getFirstOccurredAt());
        vo.setLastOccurredAt(record.getLastOccurredAt());
        vo.setAcknowledgedBy(record.getAcknowledgedBy());
        vo.setAcknowledgedAt(record.getAcknowledgedAt());
        vo.setResolvedBy(record.getResolvedBy());
        vo.setResolvedAt(record.getResolvedAt());
        vo.setResolutionRemark(record.getResolutionRemark());
        vo.setRecoveredAt(record.getRecoveredAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        Farm farm = record.getFarmId() == null ? null : farmMapper.selectById(record.getFarmId());
        Pond pond = record.getPondId() == null ? null : pondMapper.selectById(record.getPondId());
        if (farm != null) vo.setFarmName(farm.getFarmName());
        if (pond != null) vo.setPondName(pond.getPondName());
        return vo;
    }

    private void addIotAlertIfNeeded(List<IotRealtimeAlertVO> result, Pond pond, Farm farm, IotSensorData data,
                                     String field, BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return;
        boolean tooLow = min != null && value.compareTo(min) < 0;
        boolean tooHigh = max != null && value.compareTo(max) > 0;
        if (!tooLow && !tooHigh) return;
        IotRealtimeAlertVO vo = new IotRealtimeAlertVO();
        vo.setPondId(pond.getId());
        vo.setPondName(pond.getPondName());
        vo.setFarmId(pond.getFarmId());
        vo.setFarmName(farm == null ? null : farm.getFarmName());
        vo.setWaterTemp(data.getWaterTemp());
        vo.setDoLevel(data.getDissolvedOxygen());
        vo.setPhLevel(data.getPhValue());
        vo.setAlertField(field);
        vo.setCurrentValue(value);
        vo.setThresholdMin(min);
        vo.setThresholdMax(max);
        vo.setDataTime(data.getCollectTime());
        result.add(vo);
    }

    private List<BatchSurvivalSnapshot> buildSurvivalSnapshots(LocalDate startDate, LocalDate endDate, Long farmId, Long seedlingId) {
        LambdaQueryWrapper<PurchaseBatch> wrapper = new LambdaQueryWrapper<>();
        if (farmId != null) wrapper.eq(PurchaseBatch::getFarmId, farmId);
        if (seedlingId != null) wrapper.eq(PurchaseBatch::getSeedlingId, seedlingId);
        if (startDate != null) wrapper.ge(PurchaseBatch::getPurchaseDate, startDate);
        if (endDate != null) wrapper.le(PurchaseBatch::getPurchaseDate, endDate);
        wrapper.orderByDesc(PurchaseBatch::getPurchaseDate).orderByDesc(PurchaseBatch::getId);

        List<BatchSurvivalSnapshot> result = new ArrayList<>();
        for (PurchaseBatch batch : purchaseBatchMapper.selectList(wrapper)) {
            List<Stocking> stockings = stockingMapper.selectList(new LambdaQueryWrapper<Stocking>()
                    .eq(Stocking::getBatchId, batch.getId())
                    .eq(Stocking::getIsDeleted, 0));
            long stockedQty = stockings.stream().mapToLong(item -> item.getStockedQty() == null ? 0L : item.getStockedQty()).sum();
            if (stockedQty <= 0 && batch.getEstimatedTotalQty() != null) stockedQty = batch.getEstimatedTotalQty();
            if (stockedQty <= 0) continue;

            List<BatchGrowthLog> growthLogs = batchGrowthLogMapper.selectList(new LambdaQueryWrapper<BatchGrowthLog>()
                    .eq(BatchGrowthLog::getBatchNo, batch.getBatchNo()));
            long deathQty = growthLogs.stream().mapToLong(item ->
                    (item.getRoutineDeathCount() == null ? 0L : item.getRoutineDeathCount())
                            + (item.getAbnormalDeathCount() == null ? 0L : item.getAbnormalDeathCount())).sum();
            BigDecimal avgWeight = latestAvgWeight(growthLogs);

            HarvestRecord harvest = harvestRecordMapper.selectOne(new LambdaQueryWrapper<HarvestRecord>()
                    .eq(HarvestRecord::getBatchNo, batch.getBatchNo())
                    .eq(HarvestRecord::getIsDeleted, 0)
                    .last("LIMIT 1"));
            long estimatedHarvestQty = estimateHarvestQty(harvest, stockedQty, deathQty);

            BatchSurvivalSnapshot snapshot = new BatchSurvivalSnapshot();
            snapshot.batchNo = batch.getBatchNo();
            snapshot.farmId = batch.getFarmId();
            snapshot.farmName = farmName(batch.getFarmId());
            snapshot.seedlingId = batch.getSeedlingId();
            snapshot.seedlingName = seedlingName(batch.getSeedlingId());
            snapshot.stockedQty = stockedQty;
            snapshot.estimatedHarvestQty = estimatedHarvestQty;
            snapshot.deathQty = deathQty;
            snapshot.totalHarvestWeightKg = harvest == null ? BigDecimal.ZERO : nvl(harvest.getActualTotalWeightKg());
            snapshot.avgWeightG = avgWeight;
            snapshot.survivalRate = percent(estimatedHarvestQty, stockedQty);
            snapshot.analysisDate = harvest != null && harvest.getHarvestDate() != null ? harvest.getHarvestDate() : batch.getPurchaseDate();
            result.add(snapshot);
        }
        return result;
    }

    private List<ProductionSnapshot> buildProductionSnapshots(LocalDate startDate, LocalDate endDate, Long farmId, Long seedlingId) {
        LambdaQueryWrapper<HarvestRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HarvestRecord::getIsDeleted, 0);
        if (startDate != null) wrapper.ge(HarvestRecord::getHarvestDate, startDate);
        if (endDate != null) wrapper.le(HarvestRecord::getHarvestDate, endDate);
        wrapper.orderByDesc(HarvestRecord::getHarvestDate).orderByDesc(HarvestRecord::getId);

        List<ProductionSnapshot> result = new ArrayList<>();
        for (HarvestRecord harvest : harvestRecordMapper.selectList(wrapper)) {
            PurchaseBatch batch = purchaseBatchMapper.selectOne(new LambdaQueryWrapper<PurchaseBatch>()
                    .eq(PurchaseBatch::getBatchNo, harvest.getBatchNo())
                    .last("LIMIT 1"));
            Long resolvedFarmId = batch != null ? batch.getFarmId() : farmIdByPond(harvest.getPondId());
            Long resolvedSeedlingId = batch == null ? null : batch.getSeedlingId();
            if (farmId != null && !Objects.equals(farmId, resolvedFarmId)) continue;
            if (seedlingId != null && !Objects.equals(seedlingId, resolvedSeedlingId)) continue;

            ProductionSnapshot snapshot = new ProductionSnapshot();
            snapshot.farmId = resolvedFarmId;
            snapshot.farmName = farmName(resolvedFarmId);
            snapshot.seedlingId = resolvedSeedlingId;
            snapshot.seedlingName = seedlingName(resolvedSeedlingId);
            snapshot.totalProductionKg = nvl(harvest.getActualTotalWeightKg());
            snapshot.totalRevenue = nvl(harvest.getTotalRevenue());
            snapshot.totalCost = nvl(harvest.getTotalCost());
            snapshot.netProfit = nvl(harvest.getNetProfit());
            snapshot.unitPrice = harvest.getUnitPrice();
            snapshot.harvestDate = harvest.getHarvestDate();
            result.add(snapshot);
        }
        return result;
    }

    private String farmName(Long farmId) {
        if (farmId == null) return null;
        Farm farm = farmMapper.selectById(farmId);
        return farm == null ? null : farm.getFarmName();
    }

    private Long farmIdByPond(Long pondId) {
        if (pondId == null) return null;
        Pond pond = pondMapper.selectById(pondId);
        return pond == null ? null : pond.getFarmId();
    }

    private static long estimateHarvestQty(HarvestRecord harvest, long stockedQty, long deathQty) {
        if (harvest != null
                && harvest.getActualTotalWeightKg() != null
                && harvest.getActualAvgWeightG() != null
                && harvest.getActualAvgWeightG().compareTo(BigDecimal.ZERO) > 0) {
            return harvest.getActualTotalWeightKg()
                    .multiply(BigDecimal.valueOf(1000))
                    .divide(harvest.getActualAvgWeightG(), 0, RoundingMode.HALF_UP)
                    .longValue();
        }
        return Math.max(0L, stockedQty - deathQty);
    }

    private static BigDecimal latestAvgWeight(List<BatchGrowthLog> growthLogs) {
        return growthLogs.stream()
                .filter(item -> item.getAvgWeight() != null)
                .max(Comparator.comparing(BatchGrowthLog::getLogDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(BatchGrowthLog::getAvgWeight)
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) return BigDecimal.ZERO.setScale(1);
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal avg(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(1);
        return sum(values).divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal weightedAvgUnitPrice(List<ProductionSnapshot> snapshots) {
        BigDecimal production = sum(snapshots.stream().map(item -> item.totalProductionKg).toList());
        BigDecimal revenue = sum(snapshots.stream().map(item -> item.totalRevenue).toList());
        if (production.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO.setScale(2);
        return revenue.divide(production, 2, RoundingMode.HALF_UP);
    }

    private static void writeRow(XSSFSheet sheet, int rowIndex, CellStyle style, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            if (style != null) cell.setCellStyle(style);
            Object value = values[i];
            if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else if (value instanceof BigDecimal decimal) {
                cell.setCellValue(decimal.doubleValue());
            } else {
                cell.setCellValue(value == null ? "" : String.valueOf(value));
            }
        }
    }

    private static void autosize(XSSFSheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(Math.max(sheet.getColumnWidth(i), 3000), 9000));
        }
    }

    private static class BatchSurvivalSnapshot {
        String batchNo;
        Long farmId;
        String farmName;
        Long seedlingId;
        String seedlingName;
        Long stockedQty;
        Long estimatedHarvestQty;
        Long deathQty;
        BigDecimal totalHarvestWeightKg;
        BigDecimal survivalRate;
        BigDecimal avgWeightG;
        LocalDate analysisDate;
    }

    private static class ProductionSnapshot {
        Long farmId;
        String farmName;
        Long seedlingId;
        String seedlingName;
        BigDecimal totalProductionKg;
        BigDecimal totalRevenue;
        BigDecimal totalCost;
        BigDecimal netProfit;
        BigDecimal unitPrice;
        LocalDate harvestDate;
    }

    private static class SurvivalAccumulator {
        private final String key;
        private final String label;
        private Long farmId;
        private String farmName;
        private Long seedlingId;
        private String seedlingName;
        private long stockedQty;
        private long estimatedHarvestQty;
        private long deathQty;
        private BigDecimal totalHarvestWeightKg = BigDecimal.ZERO;
        private BigDecimal avgWeightSum = BigDecimal.ZERO;
        private int avgWeightCount;
        private int batchCount;

        SurvivalAccumulator(String key, String label) {
            this.key = key;
            this.label = label;
        }

        void add(BatchSurvivalSnapshot snapshot) {
            if (farmId == null) farmId = snapshot.farmId;
            if (farmName == null) farmName = snapshot.farmName;
            if (seedlingId == null) seedlingId = snapshot.seedlingId;
            if (seedlingName == null) seedlingName = snapshot.seedlingName;
            stockedQty += snapshot.stockedQty == null ? 0L : snapshot.stockedQty;
            estimatedHarvestQty += snapshot.estimatedHarvestQty == null ? 0L : snapshot.estimatedHarvestQty;
            deathQty += snapshot.deathQty == null ? 0L : snapshot.deathQty;
            totalHarvestWeightKg = totalHarvestWeightKg.add(nvl(snapshot.totalHarvestWeightKg));
            if (snapshot.avgWeightG != null && snapshot.avgWeightG.compareTo(BigDecimal.ZERO) > 0) {
                avgWeightSum = avgWeightSum.add(snapshot.avgWeightG);
                avgWeightCount++;
            }
            batchCount++;
        }

        SurvivalRateVO toVO() {
            SurvivalRateVO vo = new SurvivalRateVO();
            vo.setDimKey(key);
            vo.setDimLabel(label);
            vo.setFarmId(farmId);
            vo.setFarmName(farmName);
            vo.setSeedlingId(seedlingId);
            vo.setSeedlingName(seedlingName);
            vo.setStockedQty(stockedQty);
            vo.setEstimatedHarvestQty(estimatedHarvestQty);
            vo.setDeathQty(deathQty);
            vo.setTotalHarvestWeightKg(totalHarvestWeightKg.setScale(2, RoundingMode.HALF_UP));
            vo.setSurvivalRate(percent(estimatedHarvestQty, stockedQty));
            vo.setAvgWeightG(avgWeightCount == 0 ? BigDecimal.ZERO.setScale(1) : avgWeightSum.divide(BigDecimal.valueOf(avgWeightCount), 1, RoundingMode.HALF_UP));
            vo.setBatchCount(batchCount);
            return vo;
        }
    }

    private static String batchStatusLabel(Byte status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待检疫";
            case 1 -> "已检疫入库";
            case 2 -> "养殖中";
            case 3 -> "已出库结算";
            default -> "未知状态";
        };
    }

    private static String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String extractProvince(String address, String farmName) {
        String combined = (address == null ? "" : address) + (farmName == null ? "" : farmName);
        for (String province : PROVINCE_NAMES) {
            if (combined.contains(province)) return province;
        }
        for (String[] pair : PROVINCE_SHORT_MAP) {
            if (combined.contains(pair[0])) return pair[1];
        }
        return null;
    }

    private static final List<String> PROVINCE_NAMES = List.of(
            "广东省", "山东省", "江苏省", "浙江省", "福建省", "海南省", "辽宁省",
            "广西壮族自治区", "河北省", "河南省", "湖北省", "湖南省", "四川省",
            "安徽省", "江西省", "云南省", "贵州省", "山西省", "陕西省", "甘肃省",
            "吉林省", "黑龙江省", "青海省", "台湾省", "内蒙古自治区", "西藏自治区",
            "宁夏回族自治区", "新疆维吾尔自治区", "北京市", "天津市", "上海市", "重庆市",
            "香港特别行政区", "澳门特别行政区"
    );

    private static final String[][] PROVINCE_SHORT_MAP = {
            {"广东", "广东省"}, {"山东", "山东省"}, {"江苏", "江苏省"}, {"浙江", "浙江省"},
            {"福建", "福建省"}, {"海南", "海南省"}, {"辽宁", "辽宁省"}, {"广西", "广西壮族自治区"},
            {"河北", "河北省"}, {"河南", "河南省"}, {"湖北", "湖北省"}, {"湖南", "湖南省"},
            {"四川", "四川省"}, {"安徽", "安徽省"}, {"江西", "江西省"}, {"云南", "云南省"},
            {"贵州", "贵州省"}, {"山西", "山西省"}, {"陕西", "陕西省"}, {"甘肃", "甘肃省"},
            {"吉林", "吉林省"}, {"黑龙江", "黑龙江省"}, {"青海", "青海省"}, {"台湾", "台湾省"},
            {"内蒙古", "内蒙古自治区"}, {"西藏", "西藏自治区"}, {"宁夏", "宁夏回族自治区"},
            {"新疆", "新疆维吾尔自治区"}, {"北京", "北京市"}, {"天津", "天津市"},
            {"上海", "上海市"}, {"重庆", "重庆市"}, {"香港", "香港特别行政区"}, {"澳门", "澳门特别行政区"}
    };
    private static boolean isActiveStatus(Byte status) {
        return status != null && (status == 0 || status == 1 || status == 2);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
