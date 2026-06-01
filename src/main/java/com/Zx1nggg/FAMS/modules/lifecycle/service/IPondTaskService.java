package com.Zx1nggg.FAMS.modules.lifecycle.service;

import com.Zx1nggg.FAMS.modules.lifecycle.entity.PondTask;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.PondTaskVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface IPondTaskService extends IService<PondTask> {

    Page<PondTaskVO> pageQuery(Integer pageNum, Integer pageSize,
                               Long pondId, Long farmId, LocalDate scheduledDate,
                               Byte status, String batchNo);

    PondTaskVO queryById(Long id);

    void checkOff(Long id);

    void batchCheckOff(List<Long> ids);

    void batchDelete(List<Long> ids);

    /**
     * SOP引擎：根据投放信息自动生成未来任务清单
     */
    void generateTasks(Long batchId, Long pondId, String batchNo, Long seedlingId, LocalDate stockingDate);
}
