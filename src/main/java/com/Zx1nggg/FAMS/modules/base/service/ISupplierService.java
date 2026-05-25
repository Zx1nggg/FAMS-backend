package com.Zx1nggg.FAMS.modules.base.service;

import com.Zx1nggg.FAMS.modules.base.dto.SupplierDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Supplier;
import com.Zx1nggg.FAMS.modules.base.vo.SupplierVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ISupplierService extends IService<Supplier> {

    Page<SupplierVO> pageQuery(Integer pageNum, Integer pageSize, String supplierName);

    List<SupplierVO> listAll();

    SupplierVO queryById(Long id);

    SupplierVO create(SupplierDTO dto);

    SupplierVO update(Long id, SupplierDTO dto);

    void batchDelete(List<Long> ids);
}
