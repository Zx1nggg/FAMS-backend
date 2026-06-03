package com.Zx1nggg.FAMS.modules.lifecycle.service;

import com.Zx1nggg.FAMS.modules.lifecycle.dto.BatchGrowthLogDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.BatchGrowthLogVO;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.GrowthChartVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface IBatchGrowthLogService extends IService<BatchGrowthLog> {

    Page<BatchGrowthLogVO> pageQuery(Integer pageNum, Integer pageSize,
                                     String batchNo, Long pondId, Long farmId,
                                     Long patrolLogId,
                                     LocalDate startDate, LocalDate endDate);

    BatchGrowthLogVO queryById(Long id);

    BatchGrowthLogVO create(BatchGrowthLogDTO dto);

    BatchGrowthLogVO update(Long id, BatchGrowthLogDTO dto);

    void batchDelete(List<Long> ids);

    GrowthChartVO getGrowthChart(String batchNo, Long pondId);
}
