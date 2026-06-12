package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.dto.PurchaseBatchDTO;
import com.Zx1nggg.FAMS.modules.base.entity.PurchaseBatch;
import com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict;
import com.Zx1nggg.FAMS.modules.base.entity.Supplier;
import com.Zx1nggg.FAMS.modules.base.mapper.PurchaseBatchMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.SeedlingDictMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.SupplierMapper;
import com.Zx1nggg.FAMS.modules.base.service.IPurchaseBatchService;
import com.Zx1nggg.FAMS.modules.base.vo.PurchaseBatchVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PurchaseBatchServiceImpl extends ServiceImpl<PurchaseBatchMapper, PurchaseBatch> implements IPurchaseBatchService {

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SeedlingDictMapper seedlingDictMapper;

    private static final String BATCH_NO_PREFIX = "BN";

    @Override
    public Page<PurchaseBatchVO> pageQuery(Integer pageNum, Integer pageSize, Long farmId, Byte batchStatus, String batchNo) {
        LambdaQueryWrapper<PurchaseBatch> wrapper = new LambdaQueryWrapper<>();
        if (SecurityUtils.isFarmer()) {
            wrapper.eq(PurchaseBatch::getFarmId, SecurityUtils.getCurrentFarmId());
        } else if (farmId != null) {
            wrapper.eq(PurchaseBatch::getFarmId, farmId);
        }
        if (batchStatus != null) {
            wrapper.eq(PurchaseBatch::getBatchStatus, batchStatus);
        }
        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.like(PurchaseBatch::getBatchNo, batchNo);
        }
        wrapper.orderByDesc(PurchaseBatch::getId);
        Page<PurchaseBatch> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public PurchaseBatchVO queryById(Long id) {
        PurchaseBatch batch = getById(id);
        if (batch == null) {
            return null;
        }
        checkFarmAccess(batch);
        return toVO(batch);
    }

    @Override
    public PurchaseBatchVO create(PurchaseBatchDTO dto) {
        PurchaseBatch batch = new PurchaseBatch();
        BeanUtils.copyProperties(dto, batch);
        batch.setFarmId(resolveFarmId(dto.getFarmId()));
        batch.setBatchNo(generateBatchNo());
        batch.setEstimatedTotalQty(calcTotalQty(dto.getUnitQty(), dto.getDensityPerUnit()));
        batch.setTotalAmount(calcTotalAmount(dto.getUnitQty(), dto.getUnitPrice()));
        save(batch);
        return toVO(batch);
    }

    @Override
    public PurchaseBatchVO update(Long id, PurchaseBatchDTO dto) {
        PurchaseBatch batch = getById(id);
        if (batch == null) {
            return null;
        }
        checkFarmAccess(batch);
        assertBatchNotHarvested(batch);
        BeanUtils.copyProperties(dto, batch);
        batch.setId(id);
        batch.setFarmId(resolveFarmId(dto.getFarmId()));
        batch.setEstimatedTotalQty(calcTotalQty(dto.getUnitQty(), dto.getDensityPerUnit()));
        batch.setTotalAmount(calcTotalAmount(dto.getUnitQty(), dto.getUnitPrice()));
        updateById(batch);
        return toVO(batch);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        List<PurchaseBatch> batches = listByIds(ids);
        if (SecurityUtils.isFarmer()) {
            Long userFarmId = SecurityUtils.getCurrentFarmId();
            for (PurchaseBatch batch : batches) {
                if (!userFarmId.equals(batch.getFarmId())) {
                    throw new BusinessException(403, "无权删除不属于本养殖场的批次");
                }
            }
        }
        for (PurchaseBatch batch : batches) {
            assertBatchNotHarvested(batch);
        }
        removeByIds(ids);
    }

    private PurchaseBatchVO toVO(PurchaseBatch batch) {
        PurchaseBatchVO vo = new PurchaseBatchVO();
        BeanUtils.copyProperties(batch, vo);
        if (batch.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(batch.getSupplierId());
            if (supplier != null) {
                vo.setSupplierName(supplier.getSupplierName());
            }
        }
        if (batch.getSeedlingId() != null) {
            SeedlingDict seedling = seedlingDictMapper.selectById(batch.getSeedlingId());
            if (seedling != null) {
                vo.setSeedlingName(seedling.getCategoryName());
            }
        }
        return vo;
    }

    private Page<PurchaseBatchVO> toVOPage(Page<PurchaseBatch> page) {
        Page<PurchaseBatchVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<PurchaseBatchVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    private BigDecimal calcTotalAmount(Integer unitQty, BigDecimal unitPrice) {
        if (unitQty == null || unitPrice == null) {
            return null;
        }
        return BigDecimal.valueOf(unitQty)
                .multiply(unitPrice)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int calcTotalQty(Integer unitQty, Integer densityPerUnit) {
        if (unitQty == null || densityPerUnit == null) {
            return 0;
        }
        return unitQty * densityPerUnit;
    }

    /**
     * 生成批次号：BN + yyyyMMddHHmmss + 四位随机数字
     */
    private String generateBatchNo() {
        String dateTimePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return BATCH_NO_PREFIX + dateTimePart + randomPart;
    }

    /**
     * FARMER 用户强制使用 JWT 中的 farmId，ADMIN/REGULATOR 使用前端传入值
     */
    private Long resolveFarmId(Long dtoFarmId) {
        if (SecurityUtils.isFarmer()) {
            Long farmId = SecurityUtils.getCurrentFarmId();
            if (farmId == null) {
                throw new BusinessException(401, "当前用户养殖场信息缺失，请重新登录");
            }
            return farmId;
        }
        return dtoFarmId;
    }

    /**
     * FARMER 用户校验数据归属，禁止跨养殖场访问
     */
    private void checkFarmAccess(PurchaseBatch batch) {
        if (SecurityUtils.isFarmer()) {
            Long userFarmId = SecurityUtils.getCurrentFarmId();
            if (!userFarmId.equals(batch.getFarmId())) {
                throw new BusinessException(403, "无权访问其他养殖场的数据");
            }
        }
    }

    /**
     * 已出库结算的批次禁止编辑/删除，保护历史数据完整性
     */
    private void assertBatchNotHarvested(PurchaseBatch batch) {
        if (batch.getBatchStatus() != null && batch.getBatchStatus() == 3) {
            throw new BusinessException(400, "该批次已出库结算，历史数据不可编辑或删除");
        }
    }
}
