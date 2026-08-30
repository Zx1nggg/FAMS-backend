package com.Zx1nggg.FAMS.modules.base.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.dto.SupplierDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Supplier;
import com.Zx1nggg.FAMS.modules.base.mapper.SupplierMapper;
import com.Zx1nggg.FAMS.modules.base.service.ISupplierService;
import com.Zx1nggg.FAMS.modules.base.vo.SupplierVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements ISupplierService {

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
    public SupplierVO create(SupplierDTO dto) {
        assertCanManage();
        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(dto, supplier);
        save(supplier);
        return toVO(supplier);
    }

    @Override
    public SupplierVO update(Long id, SupplierDTO dto) {
        assertCanManage();
        Supplier supplier = getById(id);
        if (supplier == null) return null;
        BeanUtils.copyProperties(dto, supplier);
        supplier.setId(id);
        updateById(supplier);
        return toVO(supplier);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        assertCanManage();
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
        return vo;
    }

    private Page<SupplierVO> toVOPage(Page<Supplier> page) {
        Page<SupplierVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SupplierVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }
}
