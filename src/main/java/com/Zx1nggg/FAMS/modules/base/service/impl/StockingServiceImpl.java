package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.dto.StockingDTO;
import com.Zx1nggg.FAMS.modules.base.entity.*;
import com.Zx1nggg.FAMS.modules.base.mapper.*;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IPondTaskService;
import com.Zx1nggg.FAMS.modules.base.service.IStockingService;
import com.Zx1nggg.FAMS.modules.base.vo.StockingVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StockingServiceImpl extends ServiceImpl<StockingMapper, Stocking> implements IStockingService {

    @Resource
    private PurchaseBatchMapper purchaseBatchMapper;

    @Resource
    private PondMapper pondMapper;

    @Resource
    private FarmMapper farmMapper;

    @Resource
    private SeedlingDictMapper seedlingDictMapper;

    @Resource
    private IPondTaskService pondTaskService;

    @Resource
    private com.Zx1nggg.FAMS.modules.lifecycle.mapper.HarvestRecordMapper harvestRecordMapper;
    @Resource private com.Zx1nggg.FAMS.modules.lifecycle.mapper.PatrolLogMapper patrolMapper;
    @Resource private com.Zx1nggg.FAMS.modules.lifecycle.mapper.BatchGrowthLogMapper growthMapper;

    @Override
    public Page<StockingVO> pageQuery(Integer pageNum, Integer pageSize,
                                      Long farmId, Long pondId, Long batchId,
                                      LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Stocking> wrapper = new LambdaQueryWrapper<>();

        // farmId 过滤：先查出该场下所有池塘ID，再按 pond_id IN 过滤
        if (SecurityUtils.isFarmer()) {
            farmId = SecurityUtils.getCurrentFarmId();
        }
        if (farmId != null) {
            List<Pond> ponds = pondMapper.selectList(
                    new LambdaQueryWrapper<Pond>().eq(Pond::getFarmId, farmId));
            List<Long> pondIds = ponds.stream().map(Pond::getId).toList();
            if (pondIds.isEmpty()) {
                Page<StockingVO> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            wrapper.in(Stocking::getPondId, pondIds);
        }

        if (pondId != null) {
            wrapper.eq(Stocking::getPondId, pondId);
        }
        if (batchId != null) {
            wrapper.eq(Stocking::getBatchId, batchId);
        }
        if (startDate != null) {
            wrapper.ge(Stocking::getStockingDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(Stocking::getStockingDate, endDate);
        }
        wrapper.orderByDesc(Stocking::getId);
        Page<Stocking> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public StockingVO queryById(Long id) {
        Stocking stocking = getById(id);
        if (stocking == null) {
            return null;
        }
        checkFarmAccess(stocking);
        return toVO(stocking);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public StockingVO create(StockingDTO dto) {
        PurchaseBatch batch = purchaseBatchMapper.selectForUpdate(dto.getBatchId());
        if (batch == null) {
            throw new BusinessException(404, "批次不存在");
        }
        assertBatchNotHarvested(batch);
        Pond pond = pondMapper.selectById(dto.getPondId());
        if (pond == null) {
            throw new BusinessException(404, "池塘不存在");
        }

        // 校验：批次和池塘必须在同一养殖场
        if (!batch.getFarmId().equals(pond.getFarmId())) {
            throw new BusinessException(400, "批次与池塘不在同一养殖场，无法投放");
        }

        // FARMER 用户校验归属
        checkFarmAccess(batch.getFarmId());
        assertPondNotHarvested(batch, dto.getPondId());
        validateStocking(dto, batch);
        LocalDate previousFirst = firstStockingDate(batch.getId(), dto.getPondId());

        // 校验投放件数不超过批次剩余可投件数
        int alreadyStockedUnits = sumStockedUnitsByBatchId(dto.getBatchId(), null);
        int remainingUnits = batch.getUnitQty() - alreadyStockedUnits;
        if (dto.getStockedUnits() > remainingUnits) {
            throw new BusinessException(400,
                    String.format("投放件数超出批次剩余可投件数（剩余 %d %s）", remainingUnits, batch.getPurchaseUnit()));
        }

        Stocking stocking = new Stocking();
        BeanUtils.copyProperties(dto, stocking);
        // 系统自动换算尾数
        stocking.setStockedQty(dto.getStockedUnits() * batch.getDensityPerUnit());
        save(stocking);

        // 联动：首次投放时，批次状态从"已检疫入库"切换为"养殖中"
        boolean isFirstStocking = (batch.getBatchStatus() != null && batch.getBatchStatus() == 1);
        if (isFirstStocking) {
            batch.setBatchStatus((byte) 2);
            purchaseBatchMapper.updateById(batch);
        }

        // SOP引擎：首次投放时，根据苗种品种自动生成 PondTask
        long pondStockings = count(new LambdaQueryWrapper<Stocking>().eq(Stocking::getBatchId, batch.getId())
                .eq(Stocking::getPondId, dto.getPondId()));
        if (pondStockings == 1 && batch.getSeedlingId() != null) {
            pondTaskService.generateTasks(
                    batch.getId(), dto.getPondId(), batch.getBatchNo(),
                    batch.getSeedlingId(), dto.getStockingDate());
        }
        reconcileTasks(batch, dto.getPondId(), previousFirst);

        return toVO(stocking);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public StockingVO update(Long id, StockingDTO dto) {
        Stocking stocking = getById(id);
        if (stocking == null) {
            return null;
        }
        checkFarmAccess(stocking);
        if (!java.util.Objects.equals(stocking.getBatchId(), dto.getBatchId())
                || !java.util.Objects.equals(stocking.getPondId(), dto.getPondId())) {
            throw new BusinessException(400, "已有投放记录不可变更批次和池塘，请单独登记新投放");
        }

        // 检查原始关联批次是否已出库（防止通过切换批次绕过锁定）
        if (stocking.getBatchId() != null) {
            PurchaseBatch originalBatch = purchaseBatchMapper.selectById(stocking.getBatchId());
            if (originalBatch != null) {
                assertBatchNotHarvested(originalBatch);
            }
        }

        PurchaseBatch batch = purchaseBatchMapper.selectForUpdate(dto.getBatchId());
        if (batch == null) {
            throw new BusinessException(404, "批次不存在");
        }
        assertBatchNotHarvested(batch);
        checkFarmAccess(batch.getFarmId());
        assertPondNotHarvested(batch, dto.getPondId());
        validateStocking(dto, batch);
        LocalDate previousFirst = firstStockingDate(batch.getId(), dto.getPondId());
        if (!java.util.Objects.equals(stocking.getStockingDate(), dto.getStockingDate())
                || !java.util.Objects.equals(stocking.getStockedUnits(), dto.getStockedUnits())) {
            assertNoLedger(batch, dto.getPondId());
        }
        Pond pond = pondMapper.selectById(dto.getPondId());
        if (pond == null) {
            throw new BusinessException(404, "池塘不存在");
        }

        // 校验：批次和池塘必须在同一养殖场
        if (!batch.getFarmId().equals(pond.getFarmId())) {
            throw new BusinessException(400, "批次与池塘不在同一养殖场，无法投放");
        }

        // 校验投放件数（排除当前记录本身）
        int alreadyStockedUnits = sumStockedUnitsByBatchId(dto.getBatchId(), id);
        int remainingUnits = batch.getUnitQty() - alreadyStockedUnits;
        if (dto.getStockedUnits() > remainingUnits) {
            throw new BusinessException(400,
                    String.format("投放件数超出批次剩余可投件数（剩余 %d %s）", remainingUnits, batch.getPurchaseUnit()));
        }

        BeanUtils.copyProperties(dto, stocking);
        stocking.setId(id);
        // 系统自动换算尾数
        stocking.setStockedQty(dto.getStockedUnits() * batch.getDensityPerUnit());
        updateById(stocking);
        reconcileTasks(batch, dto.getPondId(), previousFirst);
        return toVO(stocking);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException(400, "请选择投放记录");
        List<Stocking> stockings = listByIds(ids);
        java.util.Map<Long, PurchaseBatch> locked = new java.util.LinkedHashMap<>();
        stockings.stream().map(Stocking::getBatchId).distinct().sorted()
                .forEach(id -> locked.put(id, purchaseBatchMapper.selectForUpdate(id)));
        java.util.Map<String, LocalDate> previousDates = new java.util.HashMap<>();
        if (SecurityUtils.isFarmer()) {
            Long userFarmId = SecurityUtils.getCurrentFarmId();
            for (Stocking stocking : stockings) {
                checkFarmAccess(stocking);
            }
        }
        for (Stocking stocking : stockings) {
            if (stocking.getBatchId() != null) {
                PurchaseBatch batch = locked.get(stocking.getBatchId());
                if (batch != null) {
                    assertBatchNotHarvested(batch);
                    assertPondNotHarvested(batch, stocking.getPondId());
                    assertNoLedger(batch, stocking.getPondId());
                    previousDates.put(batch.getId() + ":" + stocking.getPondId(), firstStockingDate(batch.getId(), stocking.getPondId()));
                }
            }
        }
        removeByIds(ids);
        java.util.Set<String> handled = new java.util.HashSet<>();
        for (Stocking stocking : stockings) {
            String key = stocking.getBatchId() + ":" + stocking.getPondId();
            if (handled.add(key)) reconcileTasks(locked.get(stocking.getBatchId()), stocking.getPondId(), previousDates.get(key));
        }
        for (PurchaseBatch batch : locked.values()) {
            if (batch != null && count(new LambdaQueryWrapper<Stocking>().eq(Stocking::getBatchId, batch.getId())) == 0) {
                batch.setBatchStatus((byte) 1); purchaseBatchMapper.updateById(batch);
            }
        }
    }

    // ==================== private helpers ====================
    private LocalDate firstStockingDate(Long batchId, Long pondId) {
        return list(new LambdaQueryWrapper<Stocking>().eq(Stocking::getBatchId, batchId).eq(Stocking::getPondId, pondId))
                .stream().map(Stocking::getStockingDate).filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null);
    }

    private void assertNoLedger(PurchaseBatch batch, Long pondId) {
        if (patrolMapper.selectCount(new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.lifecycle.entity.PatrolLog>()
                    .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.PatrolLog::getBatchNo, batch.getBatchNo())
                    .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.PatrolLog::getPondId, pondId)) > 0
                || growthMapper.selectCount(new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog>()
                    .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog::getBatchNo, batch.getBatchNo())
                    .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog::getPondId, pondId)) > 0) {
            throw new BusinessException(400, "已有巡塘或生长台账，不能更改投放数量、日期或删除投放");
        }
    }

    private void reconcileTasks(PurchaseBatch batch, Long pondId, LocalDate previousFirst) {
        if (batch == null || previousFirst == null) return;
        LocalDate currentFirst = firstStockingDate(batch.getId(), pondId);
        if (java.util.Objects.equals(previousFirst, currentFirst)) return;
        assertNoLedger(batch, pondId);
        var query = new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.lifecycle.entity.PondTask>()
                .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.PondTask::getBatchNo, batch.getBatchNo())
                .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.PondTask::getPondId, pondId);
        var existingTasks = pondTaskService.list(query);
        if (existingTasks.stream().anyMatch(t -> Byte.valueOf((byte) 1).equals(t.getStatus()))) {
            throw new BusinessException(400, "已有完成的 SOP 任务，不能更改首次投放日期或移除全部投放");
        }
        if (currentFirst == null) { pondTaskService.remove(query); return; }
        long shift = java.time.temporal.ChronoUnit.DAYS.between(previousFirst, currentFirst);
        for (var task : existingTasks) {
            if (task.getScheduledDate() != null) {
                task.setScheduledDate(task.getScheduledDate().plusDays(shift)); pondTaskService.updateById(task);
            }
        }
    }

    /**
     * 统计某个批次已投放的总件数，excludeId 用于更新时排除自身
     */
    private int sumStockedUnitsByBatchId(Long batchId, Long excludeId) {
        LambdaQueryWrapper<Stocking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stocking::getBatchId, batchId);
        if (excludeId != null) {
            wrapper.ne(Stocking::getId, excludeId);
        }
        List<Stocking> list = list(wrapper);
        return list.stream().mapToInt(s -> s.getStockedUnits() != null ? s.getStockedUnits() : 0).sum();
    }

    private StockingVO toVO(Stocking stocking) {
        StockingVO vo = new StockingVO();
        BeanUtils.copyProperties(stocking, vo);

        // 关联批次信息
        if (stocking.getBatchId() != null) {
            PurchaseBatch batch = purchaseBatchMapper.selectById(stocking.getBatchId());
            if (batch != null) {
                vo.setBatchNo(batch.getBatchNo());
                vo.setBatchStatus(batch.getBatchStatus());
                vo.setPurchaseUnit(batch.getPurchaseUnit());
                vo.setDensityPerUnit(batch.getDensityPerUnit());
                vo.setEstimatedTotalQty(batch.getEstimatedTotalQty());
                vo.setSeedlingId(batch.getSeedlingId());

                // 关联苗种名称
                if (batch.getSeedlingId() != null) {
                    SeedlingDict seedling = seedlingDictMapper.selectById(batch.getSeedlingId());
                    if (seedling != null) {
                        vo.setSeedlingName(seedling.getCategoryName());
                    }
                }
            }
        }

        // 关联池塘信息
        if (stocking.getPondId() != null) {
            Pond pond = pondMapper.selectById(stocking.getPondId());
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

        return vo;
    }

    private Page<StockingVO> toVOPage(Page<Stocking> page) {
        Page<StockingVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<StockingVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * FARMER 用户校验投放记录的归属（通过池塘 → 养殖场链路）
     */
    private void checkFarmAccess(Stocking stocking) {
        if (SecurityUtils.isFarmer()) {
            if (stocking.getPondId() == null) throw new BusinessException(403, "投放记录缺少池塘归属");
            Pond pond = pondMapper.selectById(stocking.getPondId());
            if (pond == null) throw new BusinessException(403, "池塘不存在或已删除");
            checkFarmAccess(pond.getFarmId());
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

    /**
     * 已出库结算的批次禁止新增/编辑/删除投放记录，保护历史数据完整性
     */
    private void assertBatchNotHarvested(PurchaseBatch batch) {
        if (batch.getBatchStatus() == null || (batch.getBatchStatus() != 1 && batch.getBatchStatus() != 2)) {
            throw new BusinessException(400, "仅已检疫入库或养殖中的批次可操作投放记录");
        }
    }

    private void assertPondNotHarvested(PurchaseBatch batch, Long pondId) {
        if (harvestRecordMapper.selectCount(new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord>()
                .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord::getBatchNo, batch.getBatchNo())
                .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord::getPondId, pondId)) > 0) {
            throw new BusinessException(400, "该池塘的批次已出塘，投放记录不可变更");
        }
    }

    private void validateStocking(StockingDTO dto, PurchaseBatch batch) {
        if (dto.getStockedUnits() == null || dto.getStockedUnits() <= 0 || batch.getUnitQty() == null
                || batch.getDensityPerUnit() == null || batch.getDensityPerUnit() <= 0
                || (long) dto.getStockedUnits() * batch.getDensityPerUnit() > Integer.MAX_VALUE) {
            throw new BusinessException(400, "投放件数和密度必须为正数且换算尾数不能超出整数范围");
        }
        if (dto.getStockingDate() == null || (batch.getPurchaseDate() != null && dto.getStockingDate().isBefore(batch.getPurchaseDate()))) {
            throw new BusinessException(400, "投放日期不能早于采购日期");
        }
    }
}
