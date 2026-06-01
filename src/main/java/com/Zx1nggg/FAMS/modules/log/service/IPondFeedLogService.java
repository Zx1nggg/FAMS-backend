package com.Zx1nggg.FAMS.modules.log.service;

import com.Zx1nggg.FAMS.modules.log.dto.PondFeedLogDTO;
import com.Zx1nggg.FAMS.modules.log.entity.PondFeedLog;
import com.Zx1nggg.FAMS.modules.log.vo.PondFeedLogVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface IPondFeedLogService extends IService<PondFeedLog> {

    Page<PondFeedLogVO> pageQuery(Integer pageNum, Integer pageSize,
                                  Long pondId, Long farmId, Long patrolLogId,
                                  LocalDate startDate, LocalDate endDate);

    PondFeedLogVO queryById(Long id);

    PondFeedLogVO create(PondFeedLogDTO dto);

    PondFeedLogVO update(Long id, PondFeedLogDTO dto);

    void batchDelete(List<Long> ids);
}
