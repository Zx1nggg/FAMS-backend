package com.Zx1nggg.FAMS.modules.lifecycle.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.*;
import com.Zx1nggg.FAMS.modules.base.mapper.*;
import com.Zx1nggg.FAMS.modules.lifecycle.dto.HarvestRecordDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.BatchGrowthLogMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.HarvestRecordMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IHarvestRecordService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.HarvestRecordVO;
import com.Zx1nggg.FAMS.modules.log.entity.PondFeedLog;
import com.Zx1nggg.FAMS.modules.log.mapper.PondFeedLogMapper;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class HarvestRecordServiceImpl extends ServiceImpl<HarvestRecordMapper, HarvestRecord> implements IHarvestRecordService {

    @Resource
    private PurchaseBatchMapper purchaseBatchMapper;

    @Resource
    private PondMapper pondMapper;

    @Resource
    private FarmMapper farmMapper;

    @Resource
    private SeedlingDictMapper seedlingDictMapper;

    @Resource
    private StockingMapper stockingMapper;

    @Resource
    private BatchGrowthLogMapper batchGrowthLogMapper;

    @Resource
    private PondFeedLogMapper pondFeedLogMapper;

    // ==================== 分页查询 ====================

    @Override
    public Page<HarvestRecordVO> pageQuery(Integer pageNum, Integer pageSize, Long farmId, String batchNo, Long pondId) {
        LambdaQueryWrapper<HarvestRecord> wrapper = new LambdaQueryWrapper<>();

        // 数据隔离：通过池塘 → 农场链路
        if (SecurityUtils.isFarmer()) {
            farmId = SecurityUtils.getCurrentFarmId();
        }
        if (farmId != null) {
            List<Pond> ponds = pondMapper.selectList(
                    new LambdaQueryWrapper<Pond>().eq(Pond::getFarmId, farmId));
            List<Long> pondIds = ponds.stream().map(Pond::getId).toList();
            if (pondIds.isEmpty()) {
                Page<HarvestRecordVO> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            wrapper.in(HarvestRecord::getPondId, pondIds);
        }

        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.like(HarvestRecord::getBatchNo, batchNo);
        }
        if (pondId != null) {
            wrapper.eq(HarvestRecord::getPondId, pondId);
        }
        wrapper.orderByDesc(HarvestRecord::getId);
        Page<HarvestRecord> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    // ==================== 详情 ====================

    @Override
    public HarvestRecordVO queryById(Long id) {
        HarvestRecord record = getById(id);
        if (record == null) {
            return null;
        }
        checkFarmAccess(record);
        return toVO(record);
    }

    // ==================== 创建出塘结算 ====================

    @Override
    public HarvestRecordVO create(HarvestRecordDTO dto) {
        // 1. 校验批次存在且状态为"养殖中"
        PurchaseBatch batch = purchaseBatchMapper.selectOne(
                new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, dto.getBatchNo()));
        if (batch == null) {
            throw new BusinessException(404, "批次不存在");
        }
        if (batch.getBatchStatus() == null || batch.getBatchStatus() != 2) {
            throw new BusinessException(400, "该批次当前状态不允许出塘结算（仅养殖中的批次可出塘）");
        }

        // 2. 校验一批次只能出塘一次（同 batch_no 且未删除的记录）
        Long existCount = baseMapper.selectCount(
                new LambdaQueryWrapper<HarvestRecord>().eq(HarvestRecord::getBatchNo, dto.getBatchNo()));
        if (existCount > 0) {
            throw new BusinessException(400, "该批次已出塘结算，不可重复出塘");
        }

        // 2.1 物理删除该 batch_no 下已被软删除的历史记录，释放 UNIQUE 约束
        // 场景：用户删除出塘记录后批次状态恢复为"养殖中"，再次出塘同一批次时，
        // 软删除记录的 batch_no 仍占用数据库唯一键导致 INSERT 冲突，此处做物理清除。
        baseMapper.physicalDeleteSoftDeletedByBatchNo(dto.getBatchNo());

        // 3. 校验池塘存在且与批次同养殖场
        Pond pond = pondMapper.selectById(dto.getPondId());
        if (pond == null) {
            throw new BusinessException(404, "池塘不存在");
        }
        if (!batch.getFarmId().equals(pond.getFarmId())) {
            throw new BusinessException(400, "批次与池塘不在同一养殖场");
        }

        // 4. 校验批次确实投放在该池塘
        Long stockingCount = stockingMapper.selectCount(
                new LambdaQueryWrapper<Stocking>()
                        .eq(Stocking::getBatchId, batch.getId())
                        .eq(Stocking::getPondId, dto.getPondId()));
        if (stockingCount == 0) {
            throw new BusinessException(400, "该批次未投放在所选池塘，请核对");
        }

        // 5. FARMER 权限校验
        checkFarmAccess(batch.getFarmId());

        // 6. 构建实体
        HarvestRecord record = new HarvestRecord();
        BeanUtils.copyProperties(dto, record);
        record.setOperatorId(SecurityUtils.getCurrentUserId());

        // 7. 自动回填上游成本参考值（如果 DTO 未填入）
        fillCostDefaults(dto, batch, pond);

        // 8. 自动计算金额
        calculateAmounts(record, dto);

        // 9. 自动生成溯源二维码 URL（预留）
        record.setTraceQrCodeUrl("/trace/" + dto.getBatchNo());
        record.setTraceQueryCount(0);

        save(record);

        // 10. 批次状态流转：养殖中 → 已出库结算
        batch.setBatchStatus((byte) 3);
        purchaseBatchMapper.updateById(batch);

        return toVO(record);
    }

    // ==================== 更新 ====================

    @Override
    public HarvestRecordVO update(Long id, HarvestRecordDTO dto) {
        HarvestRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(404, "出塘记录不存在");
        }
        checkFarmAccess(record);

        // 批次号不可修改
        if (!record.getBatchNo().equals(dto.getBatchNo())) {
            throw new BusinessException(400, "不允许修改关联的批次号");
        }

        // 校验池塘
        Pond pond = pondMapper.selectById(dto.getPondId());
        if (pond == null) {
            throw new BusinessException(404, "池塘不存在");
        }

        // 校验批次与池塘同场
        PurchaseBatch batch = purchaseBatchMapper.selectOne(
                new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, dto.getBatchNo()));
        if (batch != null && !batch.getFarmId().equals(pond.getFarmId())) {
            throw new BusinessException(400, "批次与池塘不在同一养殖场");
        }

        // 更新字段
        BeanUtils.copyProperties(dto, record);
        record.setId(id);

        // 自动回填上游成本参考值（如果 DTO 未填入）
        fillCostDefaults(dto, batch, pond);

        // 重新计算金额
        calculateAmounts(record, dto);

        updateById(record);
        return toVO(record);
    }

    // ==================== 批量软删除 ====================

    @Override
    public void batchDelete(List<Long> ids) {
        List<HarvestRecord> records = listByIds(ids);
        if (records.isEmpty()) {
            return;
        }

        // 逐条校验农场归属
        for (HarvestRecord record : records) {
            checkFarmAccess(record);
        }

        // MyBatis-Plus @TableLogic：removeByIds 自动转为 UPDATE is_deleted=1
        removeByIds(ids);

        // 恢复批次状态 3→2，允许重新出塘
        for (HarvestRecord record : records) {
            PurchaseBatch batch = purchaseBatchMapper.selectOne(
                    new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, record.getBatchNo()));
            if (batch != null && batch.getBatchStatus() != null && batch.getBatchStatus() == 3) {
                batch.setBatchStatus((byte) 2);
                purchaseBatchMapper.updateById(batch);
            }
        }
    }

    // ==================== 结算预览 ====================

    @Override
    public Map<String, Object> preview(Long batchId) {
        PurchaseBatch batch = purchaseBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(404, "批次不存在");
        }
        // FARMER 校验
        checkFarmAccess(batch.getFarmId());

        Map<String, Object> result = new LinkedHashMap<>();

        // 批次基本信息
        result.put("batchNo", batch.getBatchNo());
        result.put("batchStatus", batch.getBatchStatus());
        result.put("purchaseDate", batch.getPurchaseDate());
        result.put("estimatedTotalQty", batch.getEstimatedTotalQty());
        result.put("purchaseUnit", batch.getPurchaseUnit());
        result.put("unitQty", batch.getUnitQty());
        result.put("densityPerUnit", batch.getDensityPerUnit());

        // 苗种信息
        if (batch.getSeedlingId() != null) {
            SeedlingDict seedling = seedlingDictMapper.selectById(batch.getSeedlingId());
            if (seedling != null) {
                result.put("seedlingName", seedling.getCategoryName());
                result.put("growthCycleDays", seedling.getGrowthCycleDays());
                result.put("allowableMortalityRate", seedling.getAllowableMortalityRate());
            }
        }

        // 苗种成本（采购批次总金额）
        result.put("seedlingCost", batch.getTotalAmount());

        // 投放信息
        List<Stocking> stockings = stockingMapper.selectList(
                new LambdaQueryWrapper<Stocking>().eq(Stocking::getBatchId, batchId));
        if (!stockings.isEmpty()) {
            Stocking firstStocking = stockings.get(0);
            result.put("stockingDate", firstStocking.getStockingDate());

            // 累计投放尾数
            int totalStockedQty = stockings.stream()
                    .mapToInt(s -> s.getStockedQty() != null ? s.getStockedQty() : 0).sum();
            result.put("totalStockedQty", totalStockedQty);

            // 池塘信息
            Pond pond = pondMapper.selectById(firstStocking.getPondId());
            if (pond != null) {
                result.put("pondId", pond.getId());
                result.put("pondName", pond.getPondName());
                result.put("areaMu", pond.getAreaMu());

                // 养殖天数
                LocalDate stockingDate = firstStocking.getStockingDate();
                if (stockingDate != null) {
                    long cultureDays = ChronoUnit.DAYS.between(stockingDate, LocalDate.now());
                    result.put("cultureDays", cultureDays);
                }

                // 饲料投喂汇总
                if (stockingDate != null) {
                    List<PondFeedLog> feedLogs = pondFeedLogMapper.selectList(
                            new LambdaQueryWrapper<PondFeedLog>()
                                    .eq(PondFeedLog::getPondId, pond.getId())
                                    .ge(PondFeedLog::getLogDate, stockingDate));
                    BigDecimal totalFeed = feedLogs.stream()
                            .map(f -> f.getFeedAmount() != null ? f.getFeedAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.put("totalFeedKg", totalFeed);

                    // 饲料成本汇总
                    BigDecimal totalFeedCost = feedLogs.stream()
                            .map(f -> f.getFeedTotalAmount() != null ? f.getFeedTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.put("totalFeedCost", totalFeedCost.compareTo(BigDecimal.ZERO) > 0 ? totalFeedCost : null);

                    // 药品成本汇总
                    BigDecimal totalMedicineCost = feedLogs.stream()
                            .map(f -> f.getMedicineAmount() != null ? f.getMedicineAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.put("totalMedicineCost", totalMedicineCost.compareTo(BigDecimal.ZERO) > 0 ? totalMedicineCost : null);
                }
            }
        }

        // 生长抽测数据（最新一条）
        BatchGrowthLog latestGrowth = batchGrowthLogMapper.selectOne(
                new LambdaQueryWrapper<BatchGrowthLog>()
                        .eq(BatchGrowthLog::getBatchNo, batch.getBatchNo())
                        .orderByDesc(BatchGrowthLog::getLogDate)
                        .last("LIMIT 1"));
        if (latestGrowth != null) {
            result.put("latestAvgWeightG", latestGrowth.getAvgWeight());
            result.put("latestAvgLengthCm", latestGrowth.getAvgLength());
            result.put("latestLogDate", latestGrowth.getLogDate());
        }

        // 累计死亡统计
        List<BatchGrowthLog> allGrowthLogs = batchGrowthLogMapper.selectList(
                new LambdaQueryWrapper<BatchGrowthLog>().eq(BatchGrowthLog::getBatchNo, batch.getBatchNo()));
        int totalRoutineDeath = allGrowthLogs.stream()
                .mapToInt(g -> g.getRoutineDeathCount() != null ? g.getRoutineDeathCount() : 0).sum();
        int totalAbnormalDeath = allGrowthLogs.stream()
                .mapToInt(g -> g.getAbnormalDeathCount() != null ? g.getAbnormalDeathCount() : 0).sum();
        int totalDeath = totalRoutineDeath + totalAbnormalDeath;
        result.put("totalRoutineDeath", totalRoutineDeath);
        result.put("totalAbnormalDeath", totalAbnormalDeath);
        result.put("totalDeath", totalDeath);

        // 存活率 & 预估产量
        Integer stockedQty = (Integer) result.get("totalStockedQty");
        if (stockedQty != null && stockedQty > 0) {
            BigDecimal survivalRate = BigDecimal.valueOf(100L * (stockedQty - totalDeath) / stockedQty)
                    .setScale(1, RoundingMode.HALF_UP);
            result.put("survivalRate", survivalRate);

            BigDecimal avgWeight = latestGrowth != null && latestGrowth.getAvgWeight() != null
                    ? latestGrowth.getAvgWeight() : BigDecimal.ZERO;
            // 预估产量(kg) = 存活尾数 × 均重(g) / 1000
            BigDecimal predictedKg = BigDecimal.valueOf(stockedQty - totalDeath)
                    .multiply(avgWeight)
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            result.put("predictedYieldKg", predictedKg);
        }

        return result;
    }

    // ==================== Private Helpers ====================

    /**
     * 自动回填上游成本参考值：如果 DTO 中的成本字段为 null，
     * 从采购批次金额和投喂日志中汇总填入。
     */
    private void fillCostDefaults(HarvestRecordDTO dto, PurchaseBatch batch, Pond pond) {
        // 苗种成本：取采购批次总金额
        if (dto.getSeedlingCost() == null && batch.getTotalAmount() != null) {
            dto.setSeedlingCost(batch.getTotalAmount());
        }

        // 饲料成本 & 药品成本：从投喂日志汇总
        if (dto.getFeedCost() == null || dto.getMedicineCost() == null) {
            // 获取该批次的投放日期作为起始过滤
            List<Stocking> stockings = stockingMapper.selectList(
                    new LambdaQueryWrapper<Stocking>()
                            .eq(Stocking::getBatchId, batch.getId())
                            .eq(Stocking::getPondId, pond.getId()));
            LocalDate stockingDate = stockings.stream()
                    .map(Stocking::getStockingDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            LambdaQueryWrapper<PondFeedLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PondFeedLog::getPondId, pond.getId());
            if (stockingDate != null) {
                wrapper.ge(PondFeedLog::getLogDate, stockingDate);
            }
            List<PondFeedLog> feedLogs = pondFeedLogMapper.selectList(wrapper);

            if (dto.getFeedCost() == null) {
                BigDecimal feedCost = feedLogs.stream()
                        .map(f -> f.getFeedTotalAmount() != null ? f.getFeedTotalAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                dto.setFeedCost(feedCost.compareTo(BigDecimal.ZERO) > 0
                        ? feedCost.setScale(2, RoundingMode.HALF_UP) : null);
            }

            if (dto.getMedicineCost() == null) {
                BigDecimal medicineCost = feedLogs.stream()
                        .map(f -> f.getMedicineAmount() != null ? f.getMedicineAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                dto.setMedicineCost(medicineCost.compareTo(BigDecimal.ZERO) > 0
                        ? medicineCost.setScale(2, RoundingMode.HALF_UP) : null);
            }
        }
    }

    /**
     * 自动计算金额：总收入、总成本、净利润、结算状态
     */
    private void calculateAmounts(HarvestRecord record, HarvestRecordDTO dto) {
        // 总收入 = 实际总重 × 单价
        if (dto.getActualTotalWeightKg() != null && dto.getUnitPrice() != null) {
            record.setTotalRevenue(dto.getActualTotalWeightKg().multiply(dto.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            record.setTotalRevenue(null);
        }

        // 总成本 = 四项成本之和（null视为0）
        BigDecimal seedling = nvl(dto.getSeedlingCost());
        BigDecimal feed = nvl(dto.getFeedCost());
        BigDecimal medicine = nvl(dto.getMedicineCost());
        BigDecimal other = nvl(dto.getOtherCost());
        BigDecimal totalCost = seedling.add(feed).add(medicine).add(other);

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            record.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        } else {
            record.setTotalCost(null);
        }

        // 净利润 = 总收入 - 总成本
        if (record.getTotalRevenue() != null && record.getTotalCost() != null) {
            record.setNetProfit(record.getTotalRevenue().subtract(record.getTotalCost())
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            record.setNetProfit(null);
        }

        // 结算状态：DTO 显式传入则直接使用，否则根据单价+成本自动判断
        if (dto.getSettlementStatus() != null) {
            record.setSettlementStatus(dto.getSettlementStatus());
        } else {
            boolean settled = dto.getUnitPrice() != null
                    && (dto.getSeedlingCost() != null || dto.getFeedCost() != null
                        || dto.getMedicineCost() != null || dto.getOtherCost() != null);
            record.setSettlementStatus(settled ? 1 : 0);
        }
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    // ==================== VO 转换 ====================

    private HarvestRecordVO toVO(HarvestRecord record) {
        HarvestRecordVO vo = new HarvestRecordVO();
        BeanUtils.copyProperties(record, vo);

        // 关联池塘信息
        if (record.getPondId() != null) {
            Pond pond = pondMapper.selectById(record.getPondId());
            if (pond != null) {
                vo.setPondName(pond.getPondName());
                vo.setFarmId(pond.getFarmId());

                // 关联养殖场名称
                if (pond.getFarmId() != null) {
                    Farm farm = farmMapper.selectById(pond.getFarmId());
                    if (farm != null) {
                        vo.setFarmName(farm.getFarmName());
                    }
                }
            }
        }

        // 关联批次信息
        if (record.getBatchNo() != null) {
            PurchaseBatch batch = purchaseBatchMapper.selectOne(
                    new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, record.getBatchNo()));
            if (batch != null) {
                vo.setBatchStatus(batch.getBatchStatus());

                // 苗种品种名称
                if (batch.getSeedlingId() != null) {
                    SeedlingDict seedling = seedlingDictMapper.selectById(batch.getSeedlingId());
                    if (seedling != null) {
                        vo.setSeedlingName(seedling.getCategoryName());
                    }
                }

                // 关联投放信息（取该批次最早的一条投放记录）
                List<Stocking> stockings = stockingMapper.selectList(
                        new LambdaQueryWrapper<Stocking>()
                                .eq(Stocking::getBatchId, batch.getId())
                                .orderByAsc(Stocking::getStockingDate)
                                .last("LIMIT 1"));
                if (!stockings.isEmpty()) {
                    Stocking firstStocking = stockings.get(0);
                    vo.setStockedQty(firstStocking.getStockedQty());
                    vo.setStockingDate(firstStocking.getStockingDate());
                }
            }
        }

        return vo;
    }

    private Page<HarvestRecordVO> toVOPage(Page<HarvestRecord> page) {
        Page<HarvestRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<HarvestRecordVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 权限校验 ====================

    /**
     * 通过池塘 → 农场链路校验归属
     */
    private void checkFarmAccess(HarvestRecord record) {
        if (SecurityUtils.isFarmer() && record.getPondId() != null) {
            Pond pond = pondMapper.selectById(record.getPondId());
            if (pond != null) {
                checkFarmAccess(pond.getFarmId());
            }
        }
    }

    /**
     * FARMER 用户校验是否为本养殖场数据
     */
    private void checkFarmAccess(Long farmId) {
        if (SecurityUtils.isFarmer()) {
            Long userFarmId = SecurityUtils.getCurrentFarmId();
            if (!userFarmId.equals(farmId)) {
                throw new BusinessException(403, "无权操作其他养殖场的数据");
            }
        }
    }
}
