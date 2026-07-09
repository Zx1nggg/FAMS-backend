package com.Zx1nggg.FAMS.modules.lifecycle.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.entity.PurchaseBatch;
import com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict;
import com.Zx1nggg.FAMS.modules.base.entity.Stocking;
import com.Zx1nggg.FAMS.modules.base.entity.Supplier;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.PurchaseBatchMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.SeedlingDictMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.StockingMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.SupplierMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.PatrolLog;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.BatchGrowthLogMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.HarvestRecordMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.PatrolLogMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IFarmerTraceService;
import com.Zx1nggg.FAMS.modules.log.entity.PondFeedLog;
import com.Zx1nggg.FAMS.modules.log.mapper.PondFeedLogMapper;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FarmerTraceServiceImpl implements IFarmerTraceService {

    @Resource private PurchaseBatchMapper purchaseBatchMapper;
    @Resource private SupplierMapper supplierMapper;
    @Resource private SeedlingDictMapper seedlingDictMapper;
    @Resource private StockingMapper stockingMapper;
    @Resource private PondMapper pondMapper;
    @Resource private PatrolLogMapper patrolLogMapper;
    @Resource private BatchGrowthLogMapper batchGrowthLogMapper;
    @Resource private PondFeedLogMapper pondFeedLogMapper;
    @Resource private HarvestRecordMapper harvestRecordMapper;

    @Override
    public Map<String, Object> getTraceDetail(String batchNo) {
        if (batchNo == null || batchNo.isBlank()) {
            throw new BusinessException(400, "批次号不能为空");
        }

        PurchaseBatch batch = purchaseBatchMapper.selectOne(
                new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, batchNo.trim()));
        if (batch == null) {
            throw new BusinessException(404, "未找到批次号为 " + batchNo + " 的溯源数据");
        }
        checkFarmAccess(batch.getFarmId());

        List<Stocking> stockings = stockingMapper.selectList(
                new LambdaQueryWrapper<Stocking>()
                        .eq(Stocking::getBatchId, batch.getId())
                        .orderByAsc(Stocking::getStockingDate));
        Set<Long> pondIds = stockings.stream().map(Stocking::getPondId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<PatrolLog> patrols = pondIds.isEmpty()
                ? List.of()
                : patrolLogMapper.selectList(new LambdaQueryWrapper<PatrolLog>()
                        .in(PatrolLog::getPondId, pondIds)
                        .eq(PatrolLog::getBatchNo, batch.getBatchNo())
                        .orderByAsc(PatrolLog::getPatrolTime));
        Set<Long> patrolIds = patrols.stream().map(PatrolLog::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<BatchGrowthLog> growthLogs = batchGrowthLogMapper.selectList(
                new LambdaQueryWrapper<BatchGrowthLog>()
                        .eq(BatchGrowthLog::getBatchNo, batch.getBatchNo())
                        .orderByAsc(BatchGrowthLog::getLogDate));

        List<PondFeedLog> feedLogs = patrolIds.isEmpty()
                ? List.of()
                : pondFeedLogMapper.selectList(new LambdaQueryWrapper<PondFeedLog>()
                        .in(PondFeedLog::getPatrolLogId, patrolIds)
                        .orderByAsc(PondFeedLog::getLogDate));

        List<HarvestRecord> harvests = harvestRecordMapper.selectList(
                new LambdaQueryWrapper<HarvestRecord>()
                        .eq(HarvestRecord::getBatchNo, batch.getBatchNo())
                        .orderByAsc(HarvestRecord::getHarvestDate));

        Map<Long, BatchGrowthLog> growthByPatrolId = growthLogs.stream()
                .filter(g -> g.getPatrolLogId() != null)
                .collect(Collectors.toMap(BatchGrowthLog::getPatrolLogId, g -> g, (a, b) -> b, LinkedHashMap::new));
        Map<Long, BigDecimal> feedByPatrolId = new LinkedHashMap<>();
        for (PondFeedLog feed : feedLogs) {
            if (feed.getPatrolLogId() == null) continue;
            feedByPatrolId.merge(feed.getPatrolLogId(), nvl(feed.getFeedAmount()), BigDecimal::add);
        }

        List<Map<String, Object>> events = new ArrayList<>();
        events.add(purchaseEvent(batch));
        for (Stocking stocking : stockings) events.add(stockingEvent(stocking, batch));
        for (PatrolLog patrol : patrols) events.add(patrolEvent(patrol, growthByPatrolId.get(patrol.getId()), feedByPatrolId.get(patrol.getId())));
        for (HarvestRecord harvest : harvests) events.add(harvestEvent(harvest));
        events.sort(Comparator.comparing(e -> (LocalDateTime) e.get("sortTime"), Comparator.nullsLast(Comparator.naturalOrder())));
        events.forEach(e -> e.remove("sortTime"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseInfo", baseInfo(batch, stockings, growthLogs, feedLogs, harvests));
        result.put("supplier", supplierInfo(batch));
        result.put("events", events);
        return result;
    }

    private Map<String, Object> baseInfo(PurchaseBatch batch, List<Stocking> stockings, List<BatchGrowthLog> growthLogs,
                                         List<PondFeedLog> feedLogs, List<HarvestRecord> harvests) {
        int totalStocked = stockings.stream().mapToInt(s -> s.getStockedQty() != null ? s.getStockedQty() : 0).sum();
        int totalQty = totalStocked > 0 ? totalStocked : (batch.getEstimatedTotalQty() != null ? batch.getEstimatedTotalQty() : 0);
        int totalDeath = growthLogs.stream().mapToInt(g ->
                (g.getRoutineDeathCount() != null ? g.getRoutineDeathCount() : 0)
                        + (g.getAbnormalDeathCount() != null ? g.getAbnormalDeathCount() : 0)).sum();
        BigDecimal totalFeed = feedLogs.stream().map(f -> nvl(f.getFeedAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFeedCost = feedLogs.stream().map(f -> nvl(f.getFeedTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMedicineCost = feedLogs.stream().map(f -> nvl(f.getMedicineAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal survivalRate = totalQty > 0
                ? BigDecimal.valueOf(Math.max(0, Math.min(100, (totalQty - totalDeath) * 100.0 / totalQty))).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(1);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("batchNo", batch.getBatchNo());
        map.put("seedlingName", seedlingName(batch));
        map.put("totalQty", totalQty);
        map.put("survivalRate", survivalRate);
        map.put("totalFeedKg", totalFeed);
        map.put("totalDeath", totalDeath);
        map.put("status", batch.getBatchStatus());
        map.put("seedlingCost", batch.getTotalAmount());
        map.put("totalFeedCost", totalFeedCost.compareTo(BigDecimal.ZERO) > 0 ? totalFeedCost : null);
        map.put("totalMedicineCost", totalMedicineCost.compareTo(BigDecimal.ZERO) > 0 ? totalMedicineCost : null);
        map.put("traceQrCodeUrl", harvests.isEmpty() ? null : harvests.get(0).getTraceQrCodeUrl());
        return map;
    }

    private Map<String, Object> supplierInfo(PurchaseBatch batch) {
        Map<String, Object> map = new LinkedHashMap<>();
        Supplier supplier = batch.getSupplierId() == null ? null : supplierMapper.selectById(batch.getSupplierId());
        map.put("name", supplier != null ? supplier.getSupplierName() : "未知供应商");
        map.put("licenseNo", supplier != null ? supplier.getQualificationCode() : "--");
        map.put("contactPerson", supplier != null ? supplier.getContactPerson() : null);
        map.put("contactPhone", supplier != null ? supplier.getContactPhone() : null);
        map.put("quarantineNo", batch.getQuarantineCertNo() != null ? batch.getQuarantineCertNo() : "未填写检疫证");
        return map;
    }

    private Map<String, Object> purchaseEvent(PurchaseBatch batch) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("unitQty", batch.getUnitQty());
        data.put("purchaseUnit", batch.getPurchaseUnit());
        data.put("density", batch.getDensityPerUnit());
        return event("PURCHASE", "苗种采购与检疫入库", toDateTime(batch.getPurchaseDate()), "系统自动建档", data);
    }

    private Map<String, Object> stockingEvent(Stocking stocking, PurchaseBatch batch) {
        Map<String, Object> data = new LinkedHashMap<>();
        Pond pond = stocking.getPondId() == null ? null : pondMapper.selectById(stocking.getPondId());
        data.put("pondName", pond != null ? pond.getPondName() : "池塘ID:" + stocking.getPondId());
        data.put("stockedQty", stocking.getStockedQty() != null ? stocking.getStockedQty()
                : (stocking.getStockedUnits() != null && batch.getDensityPerUnit() != null ? stocking.getStockedUnits() * batch.getDensityPerUnit() : 0));
        return event("STOCKING", "投放下塘映射建立", toDateTime(stocking.getStockingDate()), "养殖场操作员", data);
    }

    private Map<String, Object> patrolEvent(PatrolLog patrol, BatchGrowthLog growth, BigDecimal feedTotal) {
        int routineDeath = growth != null && growth.getRoutineDeathCount() != null ? growth.getRoutineDeathCount() : 0;
        int abnormalDeath = growth != null && growth.getAbnormalDeathCount() != null ? growth.getAbnormalDeathCount() : 0;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("temp", patrol.getWaterTemp() != null ? patrol.getWaterTemp() : "--");
        data.put("weather", patrol.getWeather() != null ? patrol.getWeather() : "--");
        data.put("waterColor", patrol.getWaterColor() != null ? patrol.getWaterColor() : "--");
        data.put("feedTotal", feedTotal != null ? feedTotal : BigDecimal.ZERO);
        data.put("avgWeight", growth != null && growth.getAvgWeight() != null ? growth.getAvgWeight() : "--");
        data.put("routineDeath", routineDeath);
        data.put("abnormalDeath", abnormalDeath);
        data.put("deathCount", routineDeath + abnormalDeath);
        data.put("remark", patrol.getRemark() != null ? patrol.getRemark() : "完成例行水质监测与投喂。");
        return event("PATROL", "日常巡塘与监测台账", patrol.getPatrolTime() != null ? patrol.getPatrolTime() : patrol.getCreateTime(), "巡塘员", data);
    }

    private Map<String, Object> harvestEvent(HarvestRecord harvest) {
        BigDecimal weight = harvest.getActualTotalWeightKg() != null ? harvest.getActualTotalWeightKg() : BigDecimal.ZERO;
        BigDecimal avgG = harvest.getActualAvgWeightG() != null ? harvest.getActualAvgWeightG() : BigDecimal.ZERO;
        Long estimatedCount = avgG.compareTo(BigDecimal.ZERO) > 0
                ? weight.multiply(BigDecimal.valueOf(1000)).divide(avgG, 0, RoundingMode.HALF_UP).longValue()
                : null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalWeight", weight);
        data.put("avgWeightG", avgG);
        data.put("unitPrice", harvest.getUnitPrice() != null ? harvest.getUnitPrice() : BigDecimal.ZERO);
        data.put("totalRevenue", harvest.getTotalRevenue() != null ? harvest.getTotalRevenue() : BigDecimal.ZERO);
        data.put("buyer", harvest.getBuyerName() != null ? harvest.getBuyerName() : "散客/未知渠道");
        data.put("finalCount", estimatedCount);
        data.put("traceQrCodeUrl", harvest.getTraceQrCodeUrl());
        return event("HARVEST", "终点结算与出塘交易", toDateTime(harvest.getHarvestDate()), "场区主管", data);
    }

    private Map<String, Object> event(String type, String title, LocalDateTime time, String operator, Map<String, Object> data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("title", title);
        event.put("time", time);
        event.put("operator", operator);
        event.put("data", data);
        event.put("sortTime", time);
        return event;
    }

    private String seedlingName(PurchaseBatch batch) {
        if (batch.getSeedlingId() == null) return "未知水产品种";
        SeedlingDict seedling = seedlingDictMapper.selectById(batch.getSeedlingId());
        return seedling != null ? seedling.getCategoryName() : "未知水产品种";
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : LocalDateTime.of(date, LocalTime.MIN);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void checkFarmAccess(Long farmId) {
        if (SecurityUtils.isFarmer()) {
            Long currentFarmId = SecurityUtils.getCurrentFarmId();
            if (currentFarmId == null) {
                throw new BusinessException(401, "当前用户养殖场信息缺失，请重新登录");
            }
            if (!Objects.equals(currentFarmId, farmId)) {
                throw new BusinessException(403, "无权查看其他养殖场的溯源数据");
            }
        }
    }
}
