package com.Zx1nggg.FAMS.modules.base.service;

import com.Zx1nggg.FAMS.modules.base.dto.PurchaseBatchDTO;
import com.Zx1nggg.FAMS.modules.base.entity.PurchaseBatch;
import com.Zx1nggg.FAMS.modules.base.vo.PurchaseBatchVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IPurchaseBatchService extends IService<PurchaseBatch> {

    Page<PurchaseBatchVO> pageQuery(Integer pageNum, Integer pageSize, Long farmId, Byte batchStatus, String batchNo);

    PurchaseBatchVO queryById(Long id);

    PurchaseBatchVO create(PurchaseBatchDTO dto);

    PurchaseBatchVO update(Long id, PurchaseBatchDTO dto);

    void batchDelete(List<Long> ids);
}
