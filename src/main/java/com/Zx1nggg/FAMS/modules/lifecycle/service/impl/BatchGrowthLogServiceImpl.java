package com.Zx1nggg.FAMS.modules.lifecycle.service.impl;

import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.dto.BatchGrowthLogDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.BatchGrowthLogMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IBatchGrowthLogService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.BatchGrowthLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BatchGrowthLogServiceImpl extends ServiceImpl<BatchGrowthLogMapper, BatchGrowthLog> implements IBatchGrowthLogService {

    @Resource
    private PondMapper pondMapper;

    @Override
    public Page<BatchGrowthLogVO> pageQuery(Integer pageNum, Integer pageSize,
                                            String batchNo, Long pondId, Long farmId,
                                            Long patrolLogId,
                                            LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<BatchGrowthLog> wrapper = new LambdaQueryWrapper<>();

        if (farmId != null) {
            List<Pond> ponds = pondMapper.selectList(
                    new LambdaQueryWrapper<Pond>().eq(Pond::getFarmId, farmId));
            List<Long> pondIds = ponds.stream().map(Pond::getId).toList();
            if (pondIds.isEmpty()) {
                Page<BatchGrowthLogVO> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            wrapper.in(BatchGrowthLog::getPondId, pondIds);
        }
        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.eq(BatchGrowthLog::getBatchNo, batchNo);
        }
        if (pondId != null) {
            wrapper.eq(BatchGrowthLog::getPondId, pondId);
        }
        if (patrolLogId != null) {
            wrapper.eq(BatchGrowthLog::getPatrolLogId, patrolLogId);
        }
        if (startDate != null) {
            wrapper.ge(BatchGrowthLog::getLogDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(BatchGrowthLog::getLogDate, endDate);
        }
        wrapper.orderByDesc(BatchGrowthLog::getLogDate);
        Page<BatchGrowthLog> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public BatchGrowthLogVO queryById(Long id) {
        BatchGrowthLog log = getById(id);
        if (log == null) return null;
        return toVO(log);
    }

    @Override
    public BatchGrowthLogVO create(BatchGrowthLogDTO dto) {
        BatchGrowthLog log = new BatchGrowthLog();
        BeanUtils.copyProperties(dto, log);
        save(log);
        return toVO(log);
    }

    @Override
    public BatchGrowthLogVO update(Long id, BatchGrowthLogDTO dto) {
        BatchGrowthLog log = getById(id);
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

    private BatchGrowthLogVO toVO(BatchGrowthLog log) {
        BatchGrowthLogVO vo = new BatchGrowthLogVO();
        BeanUtils.copyProperties(log, vo);

        if (log.getPondId() != null) {
            Pond pond = pondMapper.selectById(log.getPondId());
            if (pond != null) vo.setPondName(pond.getPondName());
        }
        return vo;
    }

    private Page<BatchGrowthLogVO> toVOPage(Page<BatchGrowthLog> page) {
        Page<BatchGrowthLogVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
