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
import com.Zx1nggg.FAMS.modules.regulator.service.IRegulatorService;
import com.Zx1nggg.FAMS.modules.regulator.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 监管方聚合业务实现
 * 注入各模块 Mapper（非 Service）做跨模块只读聚合查询，避免循环依赖
 */
@Service
public class RegulatorServiceImpl implements IRegulatorService {

    @Autowired private FarmMapper farmMapper;
    @Autowired private PondMapper pondMapper;
    @Autowired private StockingMapper stockingMapper;
    @Autowired private HarvestRecordMapper harvestRecordMapper;
    @Autowired private PurchaseBatchMapper purchaseBatchMapper;
    @Autowired private SupplierMapper supplierMapper;
    @Autowired private SeedlingDictMapper seedlingDictMapper;
    @Autowired private BatchGrowthLogMapper batchGrowthLogMapper;
    @Autowired private PatrolLogMapper patrolLogMapper;
    @Autowired private PondFeedLogMapper pondFeedLogMapper;
    @Autowired private AlarmRecordMapper alarmRecordMapper;
    @Autowired private IotSensorDataMapper iotSensorDataMapper;

    // ==================== Dashboard Stats ====================

    @Override
    public DashboardStatsVO getDashboardStats() {
        DashboardStatsVO stats = new DashboardStatsVO();

        // 1) 入网养殖场总数（排除已逻辑删除）
        stats.setTotalFarms(farmMapper.selectCount(
                new LambdaQueryWrapper<Farm>().eq(Farm::getIsDeleted, 0)));

        // 2) 当前存栏活体总量 = SUM(stocking.stocked_qty) - 已出塘的粗略估算
        //    存栏 = 各批次总投苗尾数合计 (以万尾为单位)
        Long totalStocked = stockingMapper.selectList(
                new LambdaQueryWrapper<Stocking>().eq(Stocking::getIsDeleted, 0))
                .stream().mapToLong(s -> s.getStockedQty() != null ? s.getStockedQty() : 0).sum();
        stats.setTotalLiveStock(totalStocked / 10000);

        // 3) 本月检疫证明数 = 本月创建且 batch_status >= 1 的采购批次
        YearMonth thisMonth = YearMonth.now();
        LocalDateTime monthStart = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = thisMonth.atEndOfMonth().atTime(23, 59, 59);
        stats.setMonthlyCertificates(purchaseBatchMapper.selectCount(
                new LambdaQueryWrapper<PurchaseBatch>()
                        .ge(PurchaseBatch::getBatchStatus, 1)
                        .ge(PurchaseBatch::getPurchaseDate, monthStart.toLocalDate())
                        .le(PurchaseBatch::getPurchaseDate, monthEnd.toLocalDate())));

        // 4) 活动告警场区数（待确认、已确认、处理中）
        List<AlarmRecord> unhandledAlarms = alarmRecordMapper.selectList(
                new LambdaQueryWrapper<AlarmRecord>()
                        .in(AlarmRecord::getStatus, 0, 1, 2));
        stats.setUnhandledAlertFarms(unhandledAlarms.stream()
                .map(AlarmRecord::getFarmId).filter(Objects::nonNull).distinct().count());

        return stats;
    }

    // ==================== Dashboard Alerts ====================

    @Override
    public List<DashboardAlertVO> getDashboardAlerts(Integer limit) {
        if (limit == null || limit <= 0) limit = 5;

        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AlarmRecord::getStatus, 0, 1, 2)
                .orderByDesc(AlarmRecord::getSeverity)
                .orderByDesc(AlarmRecord::getLastOccurredAt)
                .last("LIMIT " + limit);

        List<AlarmRecord> alarms = alarmRecordMapper.selectList(wrapper);
        List<DashboardAlertVO> result = new ArrayList<>();
        for (AlarmRecord a : alarms) {
            DashboardAlertVO vo = new DashboardAlertVO();
            vo.setAlarmId(a.getId());
            vo.setFarmId(a.getFarmId());
            vo.setPondId(a.getPondId());
            vo.setAlarmCode(a.getAlarmCode());
            vo.setTitle(a.getTitle());
            vo.setMessage(a.getMessage());
            vo.setSeverity(a.getSeverity());
            vo.setStatus(a.getStatus());
            vo.setOccurrenceCount(a.getOccurrenceCount());
            vo.setLastOccurredAt(a.getLastOccurredAt());
            // fill farm name
            if (a.getFarmId() != null) {
                Farm farm = farmMapper.selectById(a.getFarmId());
                vo.setFarmName(farm != null ? farm.getFarmName() : "未知养殖场");
            } else {
                vo.setFarmName("—");
            }
            result.add(vo);
        }
        return result;
    }

    // ==================== Dashboard Watchlist ====================

    @Override
    public List<DashboardWatchlistVO> getDashboardWatchlist(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // 查询所有未删除的养殖场
        List<Farm> farms = farmMapper.selectList(
                new LambdaQueryWrapper<Farm>().eq(Farm::getIsDeleted, 0));

        List<DashboardWatchlistVO> watchlist = new ArrayList<>();
        for (Farm farm : farms) {
            // 统计近30天仍有触发的告警事件
            Long alarmCount = alarmRecordMapper.selectCount(
                    new LambdaQueryWrapper<AlarmRecord>()
                            .eq(AlarmRecord::getFarmId, farm.getId())
                            .ge(AlarmRecord::getLastOccurredAt, thirtyDaysAgo));

            // 异常死亡记录必须按当前养殖场的池塘隔离
            List<Long> farmPondIds = pondMapper.selectList(
                            new LambdaQueryWrapper<Pond>()
                                    .eq(Pond::getFarmId, farm.getId())
                                    .eq(Pond::getIsDeleted, 0))
                    .stream().map(Pond::getId).filter(Objects::nonNull).toList();
            List<BatchGrowthLog> growthLogs = farmPondIds.isEmpty()
                    ? List.of()
                    : batchGrowthLogMapper.selectList(
                            new LambdaQueryWrapper<BatchGrowthLog>()
                                    .in(BatchGrowthLog::getPondId, farmPondIds)
                                    .ge(BatchGrowthLog::getLogDate, thirtyDaysAgo.toLocalDate())
                                    .gt(BatchGrowthLog::getAbnormalDeathCount, 0));

            int totalAbnormalDeaths = growthLogs.stream()
                    .mapToInt(g -> g.getAbnormalDeathCount() != null ? g.getAbnormalDeathCount() : 0)
                    .sum();

            // 综合风险评分：告警数*3 + 异常死亡多次
            if (alarmCount > 0 || totalAbnormalDeaths > 0) {
                DashboardWatchlistVO vo = new DashboardWatchlistVO();
                vo.setFarmId(farm.getId());
                vo.setFarmName(farm.getFarmName());
                vo.setAlarmCount(alarmCount.intValue());

                if (totalAbnormalDeaths > 100) {
                    vo.setRiskType("死亡率异常");
                    vo.setRiskDescription("近30天累计异常死亡 " + totalAbnormalDeaths + " 尾，死亡率呈上升趋势");
                    vo.setRiskMetric(totalAbnormalDeaths + "尾");
                } else if (alarmCount >= 5) {
                    vo.setRiskType("环境异常");
                    vo.setRiskDescription("近30天累计触发 " + alarmCount + " 次告警，建议重点关注");
                    vo.setRiskMetric(alarmCount + "次");
                } else if (alarmCount > 0) {
                    vo.setRiskType("违规操作");
                    vo.setRiskDescription("近30天存在 " + alarmCount + " 个告警事件");
                    vo.setRiskMetric(alarmCount + "次");
                } else {
                    vo.setRiskType("环境异常");
                    vo.setRiskDescription("近30天累计异常死亡 " + totalAbnormalDeaths + " 尾");
                    vo.setRiskMetric(totalAbnormalDeaths + "尾");
                }
                watchlist.add(vo);
            }
        }

        // 按告警数+异常死亡数排序 (降序)
        watchlist.sort((a, b) -> {
            int scoreA = a.getAlarmCount() * 3;
            int scoreB = b.getAlarmCount() * 3;
            return Integer.compare(scoreB, scoreA);
        });

        return watchlist.stream().limit(limit).collect(Collectors.toList());
    }

    // ==================== GIS Farms Geo ====================

    @Override
    public List<FarmGeoVO> getFarmsGeo() {
        List<Farm> farms = farmMapper.selectList(
                new LambdaQueryWrapper<Farm>().eq(Farm::getIsDeleted, 0));

        List<FarmGeoVO> result = new ArrayList<>();
        for (Farm farm : farms) {
            FarmGeoVO vo = new FarmGeoVO();
            vo.setFarmId(farm.getId());
            vo.setFarmName(farm.getFarmName());
            vo.setLongitude(farm.getLongitude());
            vo.setLatitude(farm.getLatitude());
            vo.setAddress(farm.getAddress());
            vo.setProvince(extractProvince(farm.getAddress(), farm.getFarmName()));

            // 下属池塘：后续存栏计算必须限定在当前养殖场范围内
            List<Pond> farmPonds = pondMapper.selectList(
                    new LambdaQueryWrapper<Pond>()
                            .eq(Pond::getFarmId, farm.getId())
                            .eq(Pond::getIsDeleted, 0));
            vo.setPondCount(farmPonds.size());
            Set<Long> pondIds = farmPonds.stream()
                    .map(Pond::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 活动告警数和状态
            List<AlarmRecord> farmAlarms = alarmRecordMapper.selectList(
                    new LambdaQueryWrapper<AlarmRecord>()
                            .eq(AlarmRecord::getFarmId, farm.getId())
                            .in(AlarmRecord::getStatus, 0, 1, 2));
            vo.setActiveAlarmCount(farmAlarms.size());

            // 告警状态判定
            long criticalCount = farmAlarms.stream()
                    .filter(a -> a.getSeverity() != null && a.getSeverity() == 3).count();
            if (criticalCount > 0) {
                vo.setAlertStatus("critical");
            } else if (!farmAlarms.isEmpty()) {
                vo.setAlertStatus("warning");
            } else {
                vo.setAlertStatus("normal");
            }

            // 当前存栏 = 养殖中批次的实际投放尾数 - 对应批次累计死亡尾数
            long currentStockCount = 0L;
            if (!pondIds.isEmpty()) {
                List<PurchaseBatch> activeBatches = purchaseBatchMapper.selectList(
                        new LambdaQueryWrapper<PurchaseBatch>()
                                .eq(PurchaseBatch::getFarmId, farm.getId())
                                .eq(PurchaseBatch::getBatchStatus, (byte) 2));
                Set<Long> activeBatchIds = activeBatches.stream()
                        .map(PurchaseBatch::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                if (!activeBatchIds.isEmpty()) {
                    List<Stocking> stockings = stockingMapper.selectList(
                            new LambdaQueryWrapper<Stocking>()
                                    .in(Stocking::getPondId, pondIds)
                                    .in(Stocking::getBatchId, activeBatchIds)
                                    .eq(Stocking::getIsDeleted, 0));
                    long stockedCount = stockings.stream()
                            .mapToLong(s -> s.getStockedQty() != null ? s.getStockedQty() : 0)
                            .sum();

                    Set<Long> stockedBatchIds = stockings.stream()
                            .map(Stocking::getBatchId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    List<PurchaseBatch> stockedBatches = activeBatches.stream()
                            .filter(batch -> stockedBatchIds.contains(batch.getId()))
                            .toList();

                    // 主要品种仅取当前实际在养且已投放的批次
                    Set<String> species = new LinkedHashSet<>();
                    for (PurchaseBatch batch : stockedBatches) {
                        if (batch.getSeedlingId() == null) continue;
                        SeedlingDict seedling = seedlingDictMapper.selectById(batch.getSeedlingId());
                        if (seedling != null && seedling.getCategoryName() != null) {
                            species.add(seedling.getCategoryName());
                        }
                    }
                    vo.setMainSpecies(String.join("、", species));

                    Set<String> activeBatchNos = stockedBatches.stream()
                            .map(PurchaseBatch::getBatchNo)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    long deathCount = 0L;
                    if (!activeBatchNos.isEmpty()) {
                        List<BatchGrowthLog> growthLogs = batchGrowthLogMapper.selectList(
                                new LambdaQueryWrapper<BatchGrowthLog>()
                                        .in(BatchGrowthLog::getPondId, pondIds)
                                        .in(BatchGrowthLog::getBatchNo, activeBatchNos));
                        deathCount = growthLogs.stream().mapToLong(log ->
                                (log.getRoutineDeathCount() != null ? log.getRoutineDeathCount() : 0L)
                                        + (log.getAbnormalDeathCount() != null ? log.getAbnormalDeathCount() : 0L))
                                .sum();
                    }
                    currentStockCount = Math.max(0L, stockedCount - deathCount);
                }
            }
            // stockCount 是原始尾数；stockAmount 保留“万尾”语义兼容旧调用
            vo.setStockCount(currentStockCount);
            vo.setStockAmount(currentStockCount / 10000);

            result.add(vo);
        }
        return result;
    }

    // ==================== Trace ====================

    @Override
    public TraceChainVO quickTrace(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(400, "请输入批次号或溯源码");
        }

        // 1) 先按 batch_no 精确匹配
        PurchaseBatch batch = purchaseBatchMapper.selectOne(
                new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, keyword));
        if (batch != null) {
            return buildTraceChain(batch);
        }

        // 2) 按 harvest_record 的 trace_qr_code_url 模糊匹配
        HarvestRecord harvest = harvestRecordMapper.selectOne(
                new LambdaQueryWrapper<HarvestRecord>()
                        .like(HarvestRecord::getTraceQrCodeUrl, keyword)
                        .eq(HarvestRecord::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (harvest != null) {
            batch = purchaseBatchMapper.selectOne(
                    new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, harvest.getBatchNo()));
            if (batch != null) return buildTraceChain(batch);
        }

        // 3) 按 purchase_batch 模糊匹配 batch_no
        batch = purchaseBatchMapper.selectOne(
                new LambdaQueryWrapper<PurchaseBatch>()
                        .like(PurchaseBatch::getBatchNo, keyword)
                        .last("LIMIT 1"));
        if (batch != null) {
            return buildTraceChain(batch);
        }

        throw new BusinessException(404, "未找到该批次/溯源码的追溯信息");
    }

    @Override
    public TraceChainVO getTraceDetail(String batchNo) {
        if (batchNo == null || batchNo.isBlank()) {
            throw new BusinessException(400, "批次号不能为空");
        }
        PurchaseBatch batch = purchaseBatchMapper.selectOne(
                new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, batchNo));
        if (batch == null) {
            throw new BusinessException(404, "未找到批次 " + batchNo);
        }
        return buildTraceChain(batch);
    }

    /**
     * 根据采购批次构建完整追溯链
     */
    private TraceChainVO buildTraceChain(PurchaseBatch batch) {
        TraceChainVO chain = new TraceChainVO();
        chain.setBatchNo(batch.getBatchNo());
        List<TraceNodeVO> nodes = new ArrayList<>();

        // fill seedling name
        if (batch.getSeedlingId() != null) {
            SeedlingDict sd = seedlingDictMapper.selectById(batch.getSeedlingId());
            chain.setSeedlingName(sd != null ? sd.getCategoryName() : "未知品种");
        }

        // Node 1: Supplier (苗种来源)
        if (batch.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(batch.getSupplierId());
            if (supplier != null) {
                TraceNodeVO node = new TraceNodeVO();
                node.setNodeType("supplier");
                node.setNodeName("苗种来源");
                node.setNodeTime(supplier.getCreateTime());
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("供应商名称", supplier.getSupplierName());
                detail.put("联系人", supplier.getContactPerson());
                detail.put("联系电话", supplier.getContactPhone());
                detail.put("许可证号", supplier.getQualificationCode());
                node.setDetail(detail);
                nodes.add(node);
            }
        }

        // Node 2: Purchase (采购批次)
        {
            TraceNodeVO node = new TraceNodeVO();
            node.setNodeType("purchase");
            node.setNodeName("苗种采购");
            node.setNodeTime(batch.getPurchaseDate() != null
                    ? batch.getPurchaseDate().atStartOfDay() : null);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("批次号", batch.getBatchNo());
            detail.put("采购日期", batch.getPurchaseDate());
            detail.put("包装件数", batch.getUnitQty() + " " + (batch.getPurchaseUnit() != null ? batch.getPurchaseUnit() : ""));
            detail.put("每件密度", batch.getDensityPerUnit() + " 尾/" + batch.getPurchaseUnit());
            detail.put("预估总尾数", batch.getEstimatedTotalQty());
            detail.put("单价", batch.getUnitPrice() + " 元/" + batch.getPurchaseUnit());
            detail.put("总金额", batch.getTotalAmount() + " 元");
            detail.put("检疫证号", batch.getQuarantineCertNo());
            detail.put("批次状态", batchStatusLabel(batch.getBatchStatus()));
            node.setDetail(detail);
            nodes.add(node);
        }

        // Node 3: Stocking (投放登记)
        List<Stocking> stockings = stockingMapper.selectList(
                new LambdaQueryWrapper<Stocking>()
                        .eq(Stocking::getBatchId, batch.getId())
                        .eq(Stocking::getIsDeleted, 0));
        for (Stocking s : stockings) {
            TraceNodeVO node = new TraceNodeVO();
            node.setNodeType("stocking");
            node.setNodeName("投放登记");
            node.setNodeTime(s.getStockingDate() != null
                    ? s.getStockingDate().atStartOfDay() : s.getCreateTime());
            Map<String, Object> detail = new LinkedHashMap<>();
            // pond name
            if (s.getPondId() != null) {
                Pond pond = pondMapper.selectById(s.getPondId());
                detail.put("投放池塘", pond != null ? pond.getPondName() : "ID:" + s.getPondId());
            }
            detail.put("投放件数", s.getStockedUnits() + " 件");
            detail.put("投放尾数", s.getStockedQty() + " 尾");
            detail.put("投放总重", s.getStockedWeight() != null ? s.getStockedWeight() + " kg" : "—");
            detail.put("投放日期", s.getStockingDate());
            detail.put("备注", s.getRemark() != null ? s.getRemark() : "—");
            node.setDetail(detail);
            nodes.add(node);
        }

        // Node 4: Growth Logs (成长抽测)
        List<BatchGrowthLog> growthLogs = batchGrowthLogMapper.selectList(
                new LambdaQueryWrapper<BatchGrowthLog>()
                        .eq(BatchGrowthLog::getBatchNo, batch.getBatchNo())
                        .orderByAsc(BatchGrowthLog::getLogDate));
        for (BatchGrowthLog g : growthLogs) {
            TraceNodeVO node = new TraceNodeVO();
            node.setNodeType("growth");
            node.setNodeName("成长抽测记录");
            node.setNodeTime(g.getLogDate() != null
                    ? g.getLogDate().atStartOfDay() : null);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("抽测日期", g.getLogDate());
            if (g.getPondId() != null) {
                Pond pond = pondMapper.selectById(g.getPondId());
                detail.put("池塘", pond != null ? pond.getPondName() : "—");
            }
            detail.put("均长", g.getAvgLength() != null ? g.getAvgLength() + " cm" : "—");
            detail.put("均重", g.getAvgWeight() != null ? g.getAvgWeight() + " g" : "—");
            detail.put("日常损耗", g.getRoutineDeathCount() + " 尾");
            detail.put("异常死亡", g.getAbnormalDeathCount() != null ? g.getAbnormalDeathCount() + " 尾" : "0 尾");
            if (g.getAbnormalReason() != null) {
                detail.put("异常原因", g.getAbnormalReason());
            }
            node.setDetail(detail);
            nodes.add(node);
        }

        // Node 5: Patrol Logs (巡塘记录)
        List<PatrolLog> patrolLogs = patrolLogMapper.selectList(
                new LambdaQueryWrapper<PatrolLog>()
                        .eq(PatrolLog::getBatchNo, batch.getBatchNo())
                        .eq(PatrolLog::getIsDeleted, 0)
                        .orderByAsc(PatrolLog::getPatrolTime));
        for (PatrolLog p : patrolLogs) {
            TraceNodeVO node = new TraceNodeVO();
            node.setNodeType("patrol");
            node.setNodeName("巡塘记录");
            node.setNodeTime(p.getPatrolTime());
            Map<String, Object> detail = new LinkedHashMap<>();
            if (p.getPondId() != null) {
                Pond pond = pondMapper.selectById(p.getPondId());
                detail.put("池塘", pond != null ? pond.getPondName() : "—");
            }
            detail.put("天气", p.getWeather());
            detail.put("水温", p.getWaterTemp() != null ? p.getWaterTemp() + " °C" : "—");
            detail.put("水色", p.getWaterColor());
            detail.put("备注", p.getRemark() != null ? p.getRemark() : "—");
            node.setDetail(detail);
            nodes.add(node);
        }

        // Node 6: Feed Logs (饲料投喂)
        Set<Long> patrolLogIds = patrolLogs.stream().map(PatrolLog::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<PondFeedLog> relevantFeeds = new ArrayList<>();
        if (!patrolLogIds.isEmpty()) {
            relevantFeeds = pondFeedLogMapper.selectList(
                    new LambdaQueryWrapper<PondFeedLog>()
                            .in(PondFeedLog::getPatrolLogId, patrolLogIds)
                            .orderByAsc(PondFeedLog::getLogDate));
        }
        for (PondFeedLog f : relevantFeeds) {
            TraceNodeVO node = new TraceNodeVO();
            node.setNodeType("feed");
            node.setNodeName("饲料投喂记录");
            node.setNodeTime(f.getLogDate() != null
                    ? f.getLogDate().atStartOfDay() : null);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("投喂日期", f.getLogDate());
            detail.put("饲料品牌", f.getFeedBrand());
            detail.put("投喂量", f.getFeedAmount() != null ? f.getFeedAmount() + " kg" : "—");
            detail.put("饲料单价", f.getFeedUnitPrice() != null ? f.getFeedUnitPrice() + " 元/kg" : "—");
            if (f.getPondId() != null) {
                Pond pond = pondMapper.selectById(f.getPondId());
                detail.put("池塘", pond != null ? pond.getPondName() : "—");
            }
            detail.put("换水状态", f.getWaterChangeStatus() != null ? f.getWaterChangeStatus() : "—");
            node.setDetail(detail);
            nodes.add(node);
        }

        // Node 7: Harvest (出塘结算)
        HarvestRecord harvest = harvestRecordMapper.selectOne(
                new LambdaQueryWrapper<HarvestRecord>()
                        .eq(HarvestRecord::getBatchNo, batch.getBatchNo())
                        .eq(HarvestRecord::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (harvest != null) {
            TraceNodeVO node = new TraceNodeVO();
            node.setNodeType("harvest");
            node.setNodeName("出塘结算");
            node.setNodeTime(harvest.getHarvestDate() != null
                    ? harvest.getHarvestDate().atStartOfDay() : harvest.getCreateTime());
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("出塘日期", harvest.getHarvestDate());
            if (harvest.getPondId() != null) {
                Pond pond = pondMapper.selectById(harvest.getPondId());
                detail.put("出塘池塘", pond != null ? pond.getPondName() : "—");
            }
            detail.put("实际总重", harvest.getActualTotalWeightKg() != null
                    ? harvest.getActualTotalWeightKg() + " kg" : "—");
            detail.put("均重", harvest.getActualAvgWeightG() != null
                    ? harvest.getActualAvgWeightG() + " g/尾" : "—");
            detail.put("单价", harvest.getUnitPrice() != null
                    ? harvest.getUnitPrice() + " 元/kg" : "—");
            detail.put("总收入", harvest.getTotalRevenue() != null
                    ? harvest.getTotalRevenue() + " 元" : "—");
            detail.put("净利润", harvest.getNetProfit() != null
                    ? harvest.getNetProfit() + " 元" : "—");
            detail.put("收购方", harvest.getBuyerName());
            detail.put("结算状态", harvest.getSettlementStatus() != null
                    && harvest.getSettlementStatus() == 1 ? "已结算" : "未结算");
            node.setDetail(detail);
            nodes.add(node);
        }

        // Sort nodes by time
        nodes.sort(Comparator.nullsLast(
                Comparator.comparing(TraceNodeVO::getNodeTime, Comparator.nullsLast(Comparator.naturalOrder()))));

        chain.setNodes(nodes);
        return chain;
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

    /**
     * 从地址或养殖场名称中提取省份信息
     */
    private static String extractProvince(String address, String farmName) {
        String combined = (address != null ? address : "") + (farmName != null ? farmName : "");
        for (String province : PROVINCE_NAMES) {
            if (combined.contains(province)) {
                return province;
            }
        }
        // 尝试匹配简称
        for (String[] pair : PROVINCE_SHORT_MAP) {
            if (combined.contains(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }

    private static final List<String> PROVINCE_NAMES = List.of(
            "广东省", "山东省", "江苏省", "浙江省", "福建省", "海南省", "辽宁省",
            "广西壮族自治区", "河北省", "河南省", "湖北省", "湖南省", "四川省",
            "安徽省", "江西省", "云南省", "贵州省", "山西省", "陕西省", "甘肃省",
            "吉林省", "黑龙江省", "青海省", "台湾省",
            "内蒙古自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
            "北京市", "天津市", "上海市", "重庆市",
            "香港特别行政区", "澳门特别行政区"
    );

    private static final String[][] PROVINCE_SHORT_MAP = {
            {"广西", "广西壮族自治区"}, {"内蒙古", "内蒙古自治区"}, {"西藏", "西藏自治区"},
            {"宁夏", "宁夏回族自治区"}, {"新疆", "新疆维吾尔自治区"},
            {"北京", "北京市"}, {"天津", "天津市"}, {"上海", "上海市"}, {"重庆", "重庆市"},
            {"香港", "香港特别行政区"}, {"澳门", "澳门特别行政区"}
    };
}
