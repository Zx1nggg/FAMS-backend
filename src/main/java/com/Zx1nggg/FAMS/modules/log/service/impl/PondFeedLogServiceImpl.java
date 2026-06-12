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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class PondFeedLogServiceImpl extends ServiceImpl<PondFeedLogMapper, PondFeedLog> implements IPondFeedLogService {

    @Resource
    private PondMapper pondMapper;

    @Override
    public Page<PondFeedLogVO> pageQuery(Integer pageNum, Integer pageSize,
                                         Long pondId, Long farmId, Long patrolLogId,
                                         LocalDate startDate, LocalDate endDate) {
        // 🌟 数据隔离：FARMER 只能查看本农场的投喂日志
        if (SecurityUtils.isFarmer()) {
            farmId = SecurityUtils.getCurrentFarmId();
        }
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
        // 🌟 数据隔离
        checkFarmAccessByPondId(log.getPondId());
        return toVO(log);
    }

    @Override
    public PondFeedLogVO create(PondFeedLogDTO dto) {
        // 🌟 数据隔离
        checkFarmAccessByPondId(dto.getPondId());
        PondFeedLog log = new PondFeedLog();
        BeanUtils.copyProperties(dto, log);
        // 自动计算饲料金额
        log.setFeedTotalAmount(calcFeedAmount(dto.getFeedAmount(), dto.getFeedUnitPrice()));
        save(log);
        return toVO(log);
    }

    @Override
    public PondFeedLogVO update(Long id, PondFeedLogDTO dto) {
        PondFeedLog log = getById(id);
        if (log == null) return null;
        // 🌟 数据隔离
        checkFarmAccessByPondId(log.getPondId());
        checkFarmAccessByPondId(dto.getPondId());
        BeanUtils.copyProperties(dto, log);
        log.setId(id);
        // 自动计算饲料金额
        log.setFeedTotalAmount(calcFeedAmount(dto.getFeedAmount(), dto.getFeedUnitPrice()));
        updateById(log);
        return toVO(log);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        // 🌟 数据隔离
        if (SecurityUtils.isFarmer()) {
            List<PondFeedLog> logs = listByIds(ids);
            for (PondFeedLog log : logs) {
                checkFarmAccessByPondId(log.getPondId());
            }
        }
        removeByIds(ids);
    }

    // ==================== private helpers ====================

    /**
     * 自动计算本次投喂金额
     */
    private BigDecimal calcFeedAmount(BigDecimal feedAmount, BigDecimal unitPrice) {
        if (feedAmount == null || unitPrice == null) {
            return null;
        }
        return feedAmount.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 🌟 数据隔离：校验 FARMER 是否有权操作该池塘所属农场
     */
    private void checkFarmAccessByPondId(Long pondId) {
        if (pondId == null) return;
        if (SecurityUtils.isFarmer()) {
            Pond pond = pondMapper.selectById(pondId);
            if (pond == null || !Objects.equals(pond.getFarmId(), SecurityUtils.getCurrentFarmId())) {
                throw new BusinessException(403, "无权操作其他养殖场的数据");
            }
        }
    }

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
