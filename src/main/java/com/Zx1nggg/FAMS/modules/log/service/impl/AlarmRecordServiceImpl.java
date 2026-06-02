package com.Zx1nggg.FAMS.modules.log.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.Zx1nggg.FAMS.modules.log.entity.AlarmRecord;
import com.Zx1nggg.FAMS.modules.log.mapper.AlarmRecordMapper;
import com.Zx1nggg.FAMS.modules.log.service.IAlarmRecordService;
import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AlarmRecordServiceImpl
        extends ServiceImpl<AlarmRecordMapper, AlarmRecord>
        implements IAlarmRecordService {

    @Autowired
    private FarmMapper farmMapper;

    @Override
    public Page<AlarmRecordVO> pageQuery(Integer pageNum, Integer pageSize,
                                         Long farmId, Byte isHandled, String alarmType) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        if (farmId != null) wrapper.eq(AlarmRecord::getFarmId, farmId);
        if (isHandled != null) wrapper.eq(AlarmRecord::getIsHandled, isHandled);
        if (alarmType != null && !alarmType.isEmpty()) wrapper.eq(AlarmRecord::getAlarmType, alarmType);
        wrapper.orderByDesc(AlarmRecord::getCreateTime);

        Page<AlarmRecord> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public void handleAlarm(Long id) {
        AlarmRecord alarm = getById(id);
        if (alarm == null) throw new BusinessException(404, "告警记录不存在");
        alarm.setIsHandled((byte) 1);
        alarm.setHandleTime(LocalDateTime.now());
        updateById(alarm);
    }

    private AlarmRecordVO toVO(AlarmRecord record) {
        AlarmRecordVO vo = new AlarmRecordVO();
        BeanUtils.copyProperties(record, vo);
        if (record.getFarmId() != null) {
            Farm farm = farmMapper.selectById(record.getFarmId());
            if (farm != null) vo.setFarmName(farm.getFarmName());
        }
        return vo;
    }

    private Page<AlarmRecordVO> toVOPage(Page<AlarmRecord> page) {
        Page<AlarmRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
