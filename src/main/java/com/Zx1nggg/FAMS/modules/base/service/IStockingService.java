package com.Zx1nggg.FAMS.modules.base.service;

import com.Zx1nggg.FAMS.modules.base.dto.StockingDTO;
import com.Zx1nggg.FAMS.modules.base.entity.Stocking;
import com.Zx1nggg.FAMS.modules.base.vo.StockingVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface IStockingService extends IService<Stocking> {

    Page<StockingVO> pageQuery(Integer pageNum, Integer pageSize,
                               Long farmId, Long pondId, Long batchId,
                               LocalDate startDate, LocalDate endDate);

    StockingVO queryById(Long id);

    StockingVO create(StockingDTO dto);

    StockingVO update(Long id, StockingDTO dto);

    void batchDelete(List<Long> ids);
}
