package com.Zx1nggg.FAMS.modules.lifecycle.service;

import com.Zx1nggg.FAMS.modules.lifecycle.dto.PatrolLogDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.PatrolLog;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.PatrolLogVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface IPatrolLogService extends IService<PatrolLog> {

    Page<PatrolLogVO> pageQuery(Integer pageNum, Integer pageSize,
                                Long pondId, Long farmId,
                                LocalDate startDate, LocalDate endDate);

    PatrolLogVO queryById(Long id);

    PatrolLogVO create(PatrolLogDTO dto);

    PatrolLogVO update(Long id, PatrolLogDTO dto);

    void batchDelete(List<Long> ids);
}
