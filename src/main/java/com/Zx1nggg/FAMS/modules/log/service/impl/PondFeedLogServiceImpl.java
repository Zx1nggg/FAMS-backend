package com.Zx1nggg.FAMS.modules.log.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.log.dto.PondFeedLogDTO;
import com.Zx1nggg.FAMS.modules.log.entity.PondFeedLog;
import com.Zx1nggg.FAMS.modules.log.mapper.PondFeedLogMapper;
import com.Zx1nggg.FAMS.modules.log.service.IPondFeedLogService;
import com.Zx1nggg.FAMS.modules.log.vo.PondFeedLogVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PondFeedLogServiceImpl extends ServiceImpl<PondFeedLogMapper, PondFeedLog> implements IPondFeedLogService {

    @Resource
    private PondMapper pondMapper;

    @Override
    public Page<PondFeedLogVO> pageQuery(Integer pageNum, Integer pageSize,
                                         Long pondId, Long farmId, Long patrolLogId,
                                         LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<PondFeedLog> wrapper = new LambdaQueryWrapper<>();

        if (farmId != null) {
            List<Pond> ponds = pondMapper.selectList(
                    new LambdaQueryWrapper<Pond>().eq(Pond::getFarmId, farmId));
            List<Long> pondIds = ponds.stream().map(Pond::getId).toList();
            if (pondIds.isEmpty()) {
                Page<PondFeedLogVO> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            wrapper.in(PondFeedLog::getPondId, pondIds);
        }
        if (pondId != null) {
            wrapper.eq(PondFeedLog::getPondId, pondId);
        }
        if (patrolLogId != null) {
            wrapper.eq(PondFeedLog::getPatrolLogId, patrolLogId);
        }
        if (startDate != null) {
            wrapper.ge(PondFeedLog::getLogDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(PondFeedLog::getLogDate, endDate);
        }
        wrapper.orderByDesc(PondFeedLog::getLogDate);
        Page<PondFeedLog> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public PondFeedLogVO queryById(Long id) {
        PondFeedLog log = getById(id);
        if (log == null) return null;
        return toVO(log);
    }

    @Override
    public PondFeedLogVO create(PondFeedLogDTO dto) {
        PondFeedLog log = new PondFeedLog();
        BeanUtils.copyProperties(dto, log);
        save(log);
        return toVO(log);
    }

    @Override
    public PondFeedLogVO update(Long id, PondFeedLogDTO dto) {
        PondFeedLog log = getById(id);
        if (log == null) return null;
        BeanUtils.copyProperties(dto, log);
        log.setId(id);
        updateById(log);
        return toVO(log);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        removeByIds(ids);
    }

    // ==================== private helpers ====================

    private PondFeedLogVO toVO(PondFeedLog log) {
        PondFeedLogVO vo = new PondFeedLogVO();
        BeanUtils.copyProperties(log, vo);

        if (log.getPondId() != null) {
            Pond pond = pondMapper.selectById(log.getPondId());
            if (pond != null) vo.setPondName(pond.getPondName());
        }
        return vo;
    }

    private Page<PondFeedLogVO> toVOPage(Page<PondFeedLog> page) {
        Page<PondFeedLogVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
