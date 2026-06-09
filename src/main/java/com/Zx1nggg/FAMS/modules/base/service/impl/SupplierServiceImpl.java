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
import java.util.Objects;

@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements ISupplierService {

    @Override
    public Page<SupplierVO> pageQuery(Integer pageNum, Integer pageSize, String supplierName) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (supplierName != null && !supplierName.isEmpty()) {
            wrapper.like(Supplier::getSupplierName, supplierName);
        }
        // 🌟 数据隔离：FARMER 只能看到本农场的供应商
        if (SecurityUtils.isFarmer()) {
            wrapper.eq(Supplier::getUserId, SecurityUtils.getCurrentUserId());
        }
        wrapper.orderByDesc(Supplier::getId);
        Page<Supplier> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public List<SupplierVO> listAll() {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        // 🌟 数据隔离：FARMER 只能看到本农场的供应商
        if (SecurityUtils.isFarmer()) {
            wrapper.eq(Supplier::getUserId, SecurityUtils.getCurrentUserId());
        }
        wrapper.orderByAsc(Supplier::getSupplierName);
        List<Supplier> list = list(wrapper);
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public SupplierVO queryById(Long id) {
        Supplier supplier = getById(id);
        if (supplier == null) return null;
        // 🌟 数据隔离
        checkSupplierAccess(supplier);
        return toVO(supplier);
    }

    @Override
    public SupplierVO create(SupplierDTO dto) {
        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(dto, supplier);
        // 🌟 数据隔离：FARMER 创建时自动绑定本农场
        if (SecurityUtils.isFarmer()) {
            supplier.setUserId(SecurityUtils.getCurrentUserId());
        }
        save(supplier);
        return toVO(supplier);
    }

    @Override
    public SupplierVO update(Long id, SupplierDTO dto) {
        Supplier supplier = getById(id);
        if (supplier == null) return null;
        // 🌟 数据隔离
        checkSupplierAccess(supplier);
        BeanUtils.copyProperties(dto, supplier);
        supplier.setId(id);
        // 🌟 数据隔离：FARMER 不能将供应商转给其他农场
        if (SecurityUtils.isFarmer()) {
            supplier.setUserId(SecurityUtils.getCurrentUserId());
        }
        updateById(supplier);
        return toVO(supplier);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        // 🌟 数据隔离：FARMER 只能删除自己的供应商
        if (SecurityUtils.isFarmer()) {
            List<Supplier> suppliers = listByIds(ids);
            Long currentUserId = SecurityUtils.getCurrentUserId();
            for (Supplier s : suppliers) {
                if (!Objects.equals(s.getUserId(), currentUserId)) {
                    throw new BusinessException(403, "无权删除供应商 ID=" + s.getId());
                }
            }
        }
        removeByIds(ids);
    }

    /**
     * 🌟 数据隔离：校验 FARMER 是否拥有该供应商的操作权限
     */
    private void checkSupplierAccess(Supplier supplier) {
        if (SecurityUtils.isFarmer()
                && !Objects.equals(supplier.getUserId(), SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(403, "无权操作该供应商");
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
