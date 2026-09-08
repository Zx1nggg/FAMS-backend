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

    @Resource private com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper farmMapper;
    @Resource private com.Zx1nggg.FAMS.modules.base.mapper.StockingMapper stockingMapper;

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
    @org.springframework.transaction.annotation.Transactional
    public PurchaseBatchVO create(PurchaseBatchDTO dto) {
        validateReferences(dto, true);
        PurchaseBatch batch = new PurchaseBatch();
        BeanUtils.copyProperties(dto, batch);
        batch.setFarmId(resolveFarmId(dto.getFarmId()));
        batch.setBatchNo(generateBatchNo());
        // 采购登记不能自行声明检疫通过，状态只能由监管审核接口推进。
        batch.setBatchStatus((byte) 0);
        batch.setQuarantineCertNo(null);
        batch.setQuarantineReviewerId(null);
        batch.setQuarantineReviewedAt(null);
        batch.setEstimatedTotalQty(calcTotalQty(dto.getUnitQty(), dto.getDensityPerUnit()));
        batch.setTotalAmount(calcTotalAmount(dto.getUnitQty(), dto.getUnitPrice()));
        save(batch);
        return toVO(batch);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PurchaseBatchVO update(Long id, PurchaseBatchDTO dto) {
        PurchaseBatch batch = baseMapper.selectForUpdate(id);
        if (batch == null) {
            return null;
        }
        checkFarmAccess(batch);
        assertBatchNotHarvested(batch);
        validateReferences(dto, batch.getBatchStatus() == null || batch.getBatchStatus() == 0);
        if (dto.getBatchNo() != null && !dto.getBatchNo().equals(batch.getBatchNo())) {
            throw new BusinessException(400, "批次号不可变更");
        }
        if (batch.getBatchStatus() != null && batch.getBatchStatus() >= 1) {
            if (!java.util.Objects.equals(dto.getUnitQty(), batch.getUnitQty())
                    || !java.util.Objects.equals(dto.getDensityPerUnit(), batch.getDensityPerUnit())
                    || !java.util.Objects.equals(dto.getSeedlingId(), batch.getSeedlingId())
                    || !java.util.Objects.equals(dto.getSupplierId(), batch.getSupplierId())
                    || !java.util.Objects.equals(dto.getPurchaseUnit(), batch.getPurchaseUnit())
                    || !java.util.Objects.equals(dto.getPurchaseDate(), batch.getPurchaseDate())
                    || !java.util.Objects.equals(resolveFarmId(dto.getFarmId()), batch.getFarmId())) {
                throw new BusinessException(400, "检疫通过后不可修改供应商、苗种、数量、规格、养殖场或采购日期");
            }
        }
        String batchNo = batch.getBatchNo();
        Byte batchStatus = batch.getBatchStatus();
        String quarantineCertNo = batch.getQuarantineCertNo();
        BeanUtils.copyProperties(dto, batch);
        batch.setBatchNo(batchNo);
        // 任意普通编辑请求都不能改写监管审核结果。
        batch.setBatchStatus(batchStatus);
        batch.setQuarantineCertNo(quarantineCertNo);
        batch.setId(id);
        batch.setFarmId(resolveFarmId(dto.getFarmId()));
        batch.setEstimatedTotalQty(calcTotalQty(dto.getUnitQty(), dto.getDensityPerUnit()));
        batch.setTotalAmount(calcTotalAmount(dto.getUnitQty(), dto.getUnitPrice()));
        updateById(batch);
        return toVO(batch);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PurchaseBatchVO approveQuarantine(Long id, String quarantineCertNo) {
        if (!SecurityUtils.isRegulator() && !SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "仅监管方可执行苗种检疫审核");
        }
        PurchaseBatch batch = baseMapper.selectForUpdate(id);
        if (batch == null) return null;
        if (batch.getBatchStatus() == null || batch.getBatchStatus() != 0) {
            throw new BusinessException(400, "仅待检疫批次可签发检疫合格证明");
        }
        if (supplierMapper.countSeedlingOffering(batch.getSupplierId(), batch.getSeedlingId()) == 0) {
            throw new BusinessException(400, "该供应商已不再供应本批次苗种，不能通过检疫审核");
        }
        String certNo = quarantineCertNo == null ? null : quarantineCertNo.trim();
        if (certNo == null || certNo.isEmpty()) {
            throw new BusinessException(400, "检疫合格证号不能为空");
        }
        batch.setQuarantineCertNo(certNo);
        batch.setQuarantineReviewerId(SecurityUtils.getCurrentUserId());
        batch.setQuarantineReviewedAt(LocalDateTime.now());
        batch.setBatchStatus((byte) 1);
        updateById(batch);
        return toVO(batch);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new BusinessException(400, "请选择采购批次");
        ids.stream().sorted().forEach(baseMapper::selectForUpdate);
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
            if ((batch.getBatchStatus() != null && batch.getBatchStatus() >= 2)
                    || stockingMapper.selectCount(new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.base.entity.Stocking>()
                            .eq(com.Zx1nggg.FAMS.modules.base.entity.Stocking::getBatchId, batch.getId())) > 0) {
                throw new BusinessException(400, "已有投放的采购批次不可删除");
            }
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
        if (batch.getFarmId() != null) {
            com.Zx1nggg.FAMS.modules.base.entity.Farm farm = farmMapper.selectById(batch.getFarmId());
            if (farm != null) vo.setFarmName(farm.getFarmName());
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
        if (unitQty == null || densityPerUnit == null || unitQty <= 0 || densityPerUnit <= 0
                || (long) unitQty * densityPerUnit > Integer.MAX_VALUE) {
            throw new BusinessException(400, "采购件数和密度必须为正数且换算尾数不能超出整数范围");
        }
        return unitQty * densityPerUnit;
    }

    private void validateReferences(PurchaseBatchDTO dto, boolean requireCurrentOffering) {
        if (farmMapper.selectById(resolveFarmId(dto.getFarmId())) == null) {
            throw new BusinessException(404, "养殖场不存在");
        }
        if (dto.getSupplierId() == null || supplierMapper.selectForUpdate(dto.getSupplierId()) == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        SeedlingDict seedling = dto.getSeedlingId() == null ? null : seedlingDictMapper.selectForUpdate(dto.getSeedlingId());
        if (seedling == null) throw new BusinessException(404, "苗种不存在");
        if (requireCurrentOffering && supplierMapper.countSeedlingOffering(dto.getSupplierId(), dto.getSeedlingId()) == 0) {
            throw new BusinessException(400, "所选供应商未登记供应该苗种，请从供应商的可供应品种中选择");
        }
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
