package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.dto.SupplierDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Supplier;
import com.Zx1nggg.FAMS.modules.base.mapper.SupplierMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.SeedlingDictMapper;
import com.Zx1nggg.FAMS.modules.base.service.ISupplierService;
import com.Zx1nggg.FAMS.modules.base.vo.SupplierVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashSet;

@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements ISupplierService {

    @org.springframework.beans.factory.annotation.Autowired
    private SeedlingDictMapper seedlingDictMapper;

    @Override
    public Page<SupplierVO> pageQuery(Integer pageNum, Integer pageSize, String supplierName) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (supplierName != null && !supplierName.isEmpty()) {
            wrapper.like(Supplier::getSupplierName, supplierName);
        }
        // 🌟 供应商名录为全局目录，由监督方统一维护，农户仅可查看
        wrapper.orderByDesc(Supplier::getId);
        Page<Supplier> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public List<SupplierVO> listAll() {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Supplier::getSupplierName);
        List<Supplier> list = list(wrapper);
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public SupplierVO queryById(Long id) {
        Supplier supplier = getById(id);
        if (supplier == null) return null;
        return toVO(supplier);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public SupplierVO create(SupplierDTO dto) {
        assertCanManage();
        List<Long> seedlingIds = validateSeedlingIds(dto.getSeedlingIds());
        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(dto, supplier);
        save(supplier);
        replaceSeedlingOfferings(supplier.getId(), seedlingIds);
        return toVO(supplier);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public SupplierVO update(Long id, SupplierDTO dto) {
        assertCanManage();
        Supplier supplier = getById(id);
        if (supplier == null) return null;
        List<Long> seedlingIds = validateSeedlingIds(dto.getSeedlingIds());
        BeanUtils.copyProperties(dto, supplier);
        supplier.setId(id);
        updateById(supplier);
        replaceSeedlingOfferings(id, seedlingIds);
        return toVO(supplier);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void batchDelete(List<Long> ids) {
        assertCanManage();
        if (ids == null || ids.isEmpty()) throw new BusinessException(400, "请选择供应商");
        ids.stream().distinct().sorted().forEach(baseMapper::selectForUpdate);
        for (Long id : ids) {
            if (baseMapper.countReferences(id) > 0) throw new BusinessException(400, "供应商被采购记录引用，不能删除");
            baseMapper.deleteSeedlingOfferings(id);
        }
        removeByIds(ids);
    }

    /**
     * 🌟 权限守卫：供应商名录仅监督方(REGULATOR)或管理员(ADMIN)可维护，农户只读
     */
    private void assertCanManage() {
        if (!SecurityUtils.isRegulator() && !SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "仅监督方可维护供应商名录");
        }
    }

    private SupplierVO toVO(Supplier supplier) {
        SupplierVO vo = new SupplierVO();
        BeanUtils.copyProperties(supplier, vo);
        List<Long> seedlingIds = baseMapper.selectSeedlingIds(supplier.getId());
        vo.setSeedlingIds(seedlingIds);
        vo.setSeedlingNames(seedlingIds.stream()
                .map(seedlingDictMapper::selectById)
                .filter(java.util.Objects::nonNull)
                .map(com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict::getCategoryName)
                .toList());
        return vo;
    }

    private List<Long> validateSeedlingIds(List<Long> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            throw new BusinessException(400, "请至少选择一个可供应苗种");
        }
        List<Long> ids = new java.util.ArrayList<>(new LinkedHashSet<>(rawIds));
        if (ids.stream().anyMatch(java.util.Objects::isNull)) {
            throw new BusinessException(400, "可供应苗种不能为空");
        }
        long existing = seedlingDictMapper.selectCount(
                new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict>()
                        .in(com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict::getId, ids));
        if (existing != ids.size()) {
            throw new BusinessException(404, "所选可供应苗种中包含不存在的品类");
        }
        return ids;
    }

    private void replaceSeedlingOfferings(Long supplierId, List<Long> seedlingIds) {
        baseMapper.deleteSeedlingOfferings(supplierId);
        seedlingIds.forEach(seedlingId -> baseMapper.insertSeedlingOffering(supplierId, seedlingId));
    }

    private Page<SupplierVO> toVOPage(Page<Supplier> page) {
        Page<SupplierVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SupplierVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }
}
