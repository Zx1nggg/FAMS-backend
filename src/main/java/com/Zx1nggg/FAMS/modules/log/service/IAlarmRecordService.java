package com.Zx1nggg.FAMS.modules.log.service;

import com.Zx1nggg.FAMS.modules.log.entity.AlarmRecord;
import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 告警记录服务接口
 */
public interface IAlarmRecordService extends IService<AlarmRecord> {

    Page<AlarmRecordVO> pageQuery(Integer pageNum, Integer pageSize,
                                  Long farmId, Byte isHandled, String alarmType);

    void handleAlarm(Long id);
}
