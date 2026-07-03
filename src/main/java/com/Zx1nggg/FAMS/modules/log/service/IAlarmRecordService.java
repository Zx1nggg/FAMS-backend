package com.Zx1nggg.FAMS.modules.log.service;

import com.Zx1nggg.FAMS.modules.log.entity.AlarmRecord;
import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IAlarmRecordService extends IService<AlarmRecord> {
    Page<AlarmRecordVO> pageQuery(Integer pageNum, Integer pageSize, Long farmId, Long pondId, Boolean activeOnly,
                                  Byte status, Byte severity, String alarmCode);

    void acknowledge(Long id, String remark);
    void startProcessing(Long id, String remark);
    void resolve(Long id, String remark);
    void close(Long id, String remark);
    void reopen(Long id, String remark);
}