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
    public StockingVO create(StockingDTO dto) {
        PurchaseBatch batch = purchaseBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BusinessException(404, "批次不存在");
        }
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
        if (isFirstStocking && batch.getSeedlingId() != null) {
            pondTaskService.generateTasks(
                    batch.getId(), dto.getPondId(), batch.getBatchNo(),
                    batch.getSeedlingId(), dto.getStockingDate());
        }

        return toVO(stocking);
    }

    @Override
    public StockingVO update(Long id, StockingDTO dto) {
        Stocking stocking = getById(id);
        if (stocking == null) {
            return null;
        }
        checkFarmAccess(stocking);

        PurchaseBatch batch = purchaseBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BusinessException(404, "批次不存在");
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
        return toVO(stocking);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (SecurityUtils.isFarmer()) {
            List<Stocking> stockings = listByIds(ids);
            Long userFarmId = SecurityUtils.getCurrentFarmId();
            for (Stocking stocking : stockings) {
                checkFarmAccess(stocking);
            }
        }
        removeByIds(ids);
    }

    // ==================== private helpers ====================

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
        if (SecurityUtils.isFarmer() && stocking.getPondId() != null) {
            Pond pond = pondMapper.selectById(stocking.getPondId());
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
