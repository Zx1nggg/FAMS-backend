package com.Zx1nggg.FAMS.modules.log.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.log.entity.AlarmActionLog;
import com.Zx1nggg.FAMS.modules.log.entity.AlarmRecord;
import com.Zx1nggg.FAMS.modules.log.mapper.AlarmActionLogMapper;
import com.Zx1nggg.FAMS.modules.log.mapper.AlarmRecordMapper;
import com.Zx1nggg.FAMS.modules.log.service.IAlarmRecordService;
import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.mapper.UserMapper;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Service
public class AlarmRecordServiceImpl extends ServiceImpl<AlarmRecordMapper, AlarmRecord>
        implements IAlarmRecordService {

    private static final byte PENDING = 0;
    private static final byte ACKNOWLEDGED = 1;
    private static final byte PROCESSING = 2;
    private static final byte RESOLVED = 3;
    private static final byte CLOSED = 4;

    @Autowired private FarmMapper farmMapper;
    @Autowired private PondMapper pondMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private AlarmActionLogMapper actionLogMapper;

    @Override
    public Page<AlarmRecordVO> pageQuery(Integer pageNum, Integer pageSize, Long farmId, Long pondId, Boolean activeOnly,
                                         Byte status, Byte severity, String alarmCode) {
        if (SecurityUtils.isFarmer()) {
            farmId = SecurityUtils.getCurrentFarmId();
            if (farmId == null) throw new BusinessException(401, "当前用户养殖场信息缺失，请重新登录");
        }

        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        if (farmId != null) wrapper.eq(AlarmRecord::getFarmId, farmId);
        if (pondId != null) wrapper.eq(AlarmRecord::getPondId, pondId);
        if (Boolean.TRUE.equals(activeOnly)) wrapper.in(AlarmRecord::getStatus, 0, 1, 2);
        else if (status != null) wrapper.eq(AlarmRecord::getStatus, status);
        if (severity != null) wrapper.eq(AlarmRecord::getSeverity, severity);
        if (alarmCode != null && !alarmCode.isBlank()) wrapper.eq(AlarmRecord::getAlarmCode, alarmCode);
        wrapper.orderByAsc(AlarmRecord::getStatus)
                .orderByDesc(AlarmRecord::getSeverity)
                .orderByDesc(AlarmRecord::getLastOccurredAt);

        Page<AlarmRecord> page = page(new Page<>(pageNum, pageSize), wrapper);
        Page<AlarmRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public void acknowledge(Long id, String remark) {
        transition(id, Set.of(PENDING), ACKNOWLEDGED, "ACKNOWLEDGE", remark);
    }

    @Override
    @Transactional
    public void startProcessing(Long id, String remark) {
        transition(id, Set.of(PENDING, ACKNOWLEDGED), PROCESSING, "START_PROCESS", remark);
    }

    @Override
    @Transactional
    public void resolve(Long id, String remark) {
        if (remark == null || remark.isBlank()) throw new BusinessException(400, "解决告警时必须填写处理说明");
        transition(id, Set.of(PENDING, ACKNOWLEDGED, PROCESSING), RESOLVED, "RESOLVE", remark);
    }

    @Override
    @Transactional
    public void close(Long id, String remark) {
        transition(id, Set.of(RESOLVED), CLOSED, "CLOSE", remark);
    }

    @Override
    @Transactional
    public void reopen(Long id, String remark) {
        transition(id, Set.of(RESOLVED, CLOSED), PENDING, "REOPEN", remark);
    }

    private void transition(Long id, Set<Byte> allowedFrom, byte target, String actionType, String remark) {
        AlarmRecord alarm = getById(id);
        if (alarm == null) throw new BusinessException(404, "告警记录不存在");
        checkFarmAccess(alarm);

        byte current = alarm.getStatus() == null ? PENDING : alarm.getStatus();
        if (!allowedFrom.contains(current)) {
            throw new BusinessException(400, "告警当前状态不允许执行该操作");
        }

        Long operatorId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        if ((target == ACKNOWLEDGED || target == PROCESSING || target == RESOLVED)
                && alarm.getAcknowledgedAt() == null) {
            alarm.setAcknowledgedBy(operatorId);
            alarm.setAcknowledgedAt(now);
        }
        if (target == RESOLVED) {
            alarm.setResolvedBy(operatorId);
            alarm.setResolvedAt(now);
            alarm.setResolutionRemark(remark);
        } else if (target == PENDING) {
            alarm.setAcknowledgedBy(null);
            alarm.setAcknowledgedAt(null);
            alarm.setResolvedBy(null);
            alarm.setResolvedAt(null);
            alarm.setResolutionRemark(null);
            alarm.setRecoveredAt(null);
        }
        alarm.setStatus(target);
        updateById(alarm);

        AlarmActionLog action = new AlarmActionLog();
        action.setAlarmId(alarm.getId());
        action.setActionType(actionType);
        action.setFromStatus(current);
        action.setToStatus(target);
        action.setOperatorId(operatorId);
        action.setActionRemark(remark);
        action.setCreatedAt(now);
        actionLogMapper.insert(action);
    }

    private void checkFarmAccess(AlarmRecord alarm) {
        if (SecurityUtils.isFarmer()
                && !Objects.equals(alarm.getFarmId(), SecurityUtils.getCurrentFarmId())) {
            throw new BusinessException(403, "无权查看或处理其他养殖场的告警");
        }
    }

    private AlarmRecordVO toVO(AlarmRecord record) {
        AlarmRecordVO vo = new AlarmRecordVO();
        BeanUtils.copyProperties(record, vo);
        if (record.getFarmId() != null) {
            Farm farm = farmMapper.selectById(record.getFarmId());
            if (farm != null) vo.setFarmName(farm.getFarmName());
        }
        if (record.getPondId() != null) {
            Pond pond = pondMapper.selectById(record.getPondId());
            if (pond != null) vo.setPondName(pond.getPondName());
        }
        if (record.getAcknowledgedBy() != null) {
            User user = userMapper.selectById(record.getAcknowledgedBy());
            if (user != null) vo.setAcknowledgedByName(user.getRealName());
        }
        if (record.getResolvedBy() != null) {
            User user = userMapper.selectById(record.getResolvedBy());
            if (user != null) vo.setResolvedByName(user.getRealName());
        }
        return vo;
    }
}