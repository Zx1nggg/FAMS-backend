package com.Zx1nggg.FAMS.modules.lifecycle.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.entity.PurchaseBatch;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.PurchaseBatchMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.dto.PatrolLogDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.PatrolLog;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.PatrolLogMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IPatrolLogService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.PatrolLogVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class PatrolLogServiceImpl extends ServiceImpl<PatrolLogMapper, PatrolLog> implements IPatrolLogService {

    @Resource
    private PondMapper pondMapper;

    @Resource
    private FarmMapper farmMapper;

    @Resource
    private PurchaseBatchMapper purchaseBatchMapper;

    @Resource
    private com.Zx1nggg.FAMS.modules.lifecycle.service.LifecycleAccessService lifecycleAccess;
    @Resource
    private com.Zx1nggg.FAMS.modules.lifecycle.mapper.BatchGrowthLogMapper growthMapper;
    @Resource
    private com.Zx1nggg.FAMS.modules.log.mapper.PondFeedLogMapper feedMapper;

    @Override
    public Page<PatrolLogVO> pageQuery(Integer pageNum, Integer pageSize,
                                       Long pondId, Long farmId,
                                       LocalDate startDate, LocalDate endDate) {
        // 🌟 数据隔离：FARMER 只能查看本农场的巡塘日志
        if (SecurityUtils.isFarmer()) {
            farmId = SecurityUtils.getCurrentFarmId();
        }
        LambdaQueryWrapper<PatrolLog> wrapper = new LambdaQueryWrapper<>();

        if (farmId != null) {
            List<Pond> ponds = pondMapper.selectList(
                    new LambdaQueryWrapper<Pond>().eq(Pond::getFarmId, farmId));
            List<Long> pondIds = ponds.stream().map(Pond::getId).toList();
            if (pondIds.isEmpty()) {
                Page<PatrolLogVO> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            wrapper.in(PatrolLog::getPondId, pondIds);
        }
        if (pondId != null) {
            wrapper.eq(PatrolLog::getPondId, pondId);
        }
        if (startDate != null) {
            wrapper.ge(PatrolLog::getPatrolTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(PatrolLog::getPatrolTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        wrapper.orderByDesc(PatrolLog::getPatrolTime);
        Page<PatrolLog> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public PatrolLogVO queryById(Long id) {
        PatrolLog log = getById(id);
        if (log == null) return null;
        checkFarmAccess(log);
        return toVO(log);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PatrolLogVO create(PatrolLogDTO dto) {
        validateAssociation(dto);
        Pond pond = pondMapper.selectById(dto.getPondId());
        if (pond == null) throw new BusinessException(404, "池塘不存在");

        checkFarmAccessByPond(pond);
        assertBatchNotHarvested(dto.getBatchNo());

        PatrolLog log = new PatrolLog();
        BeanUtils.copyProperties(dto, log);
        log.setOperatorId(SecurityUtils.getCurrentUserId());
        save(log);
        return toVO(log);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PatrolLogVO update(Long id, PatrolLogDTO dto) {
        PatrolLog log = getById(id);
        if (log == null) return null;
        checkFarmAccess(log);
        lifecycleAccess.requirePatrol(id, log.getPondId(), log.getBatchNo());
        if (!java.util.Objects.equals(log.getPondId(), dto.getPondId())
                || !java.util.Objects.equals(log.getBatchNo(), dto.getBatchNo())) {
            assertNoChildren(id);
        }
        assertBatchNotHarvested(log.getBatchNo());
        validateAssociation(dto);

        Pond pond = pondMapper.selectById(dto.getPondId());
        if (pond == null) throw new BusinessException(404, "池塘不存在");

        BeanUtils.copyProperties(dto, log);
        log.setId(id);
        updateById(log);
        return toVO(log);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void batchDelete(List<Long> ids) {
        List<PatrolLog> logs = listByIds(ids);
        if (SecurityUtils.isFarmer()) {
            for (PatrolLog log : logs) {
                checkFarmAccess(log);
            }
        }
        for (PatrolLog log : logs) {
            lifecycleAccess.requirePatrol(log.getId(), log.getPondId(), log.getBatchNo());
            assertNoChildren(log.getId());
        }
        removeByIds(ids);
    }

    // ==================== private helpers ====================
    private void assertNoChildren(Long id) {
        if (growthMapper.selectCount(new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog>()
                .eq(com.Zx1nggg.FAMS.modules.lifecycle.entity.BatchGrowthLog::getPatrolLogId, id)) > 0
                || feedMapper.selectCount(new LambdaQueryWrapper<com.Zx1nggg.FAMS.modules.log.entity.PondFeedLog>()
                .eq(com.Zx1nggg.FAMS.modules.log.entity.PondFeedLog::getPatrolLogId, id)) > 0) {
            throw new BusinessException(400, "巡塘记录存在生长或投喂明细，请先处理关联记录");
        }
    }

    private PatrolLogVO toVO(PatrolLog log) {
        PatrolLogVO vo = new PatrolLogVO();
        BeanUtils.copyProperties(log, vo);

        if (log.getPondId() != null) {
            Pond pond = pondMapper.selectById(log.getPondId());
            if (pond != null) {
                vo.setPondName(pond.getPondName());
                vo.setFarmId(pond.getFarmId());
                if (pond.getFarmId() != null) {
                    Farm farm = farmMapper.selectById(pond.getFarmId());
                    if (farm != null) vo.setFarmName(farm.getFarmName());
                }
            }
        }
        return vo;
    }

    private Page<PatrolLogVO> toVOPage(Page<PatrolLog> page) {
        Page<PatrolLogVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    private void checkFarmAccess(PatrolLog log) {
        lifecycleAccess.requirePond(log.getPondId());
    }

    private void validateAssociation(PatrolLogDTO dto) {
        lifecycleAccess.requirePond(dto.getPondId());
        if (dto.getBatchNo() != null && !dto.getBatchNo().isBlank()) {
            lifecycleAccess.requireBatchForPond(dto.getBatchNo(), dto.getPondId(), true);
        }
    }

    private void checkFarmAccessByPond(Pond pond) {
        if (SecurityUtils.isFarmer()) {
            Long userFarmId = SecurityUtils.getCurrentFarmId();
            if (!userFarmId.equals(pond.getFarmId())) {
                throw new BusinessException(403, "无权操作其他养殖场的数据");
            }
        }
    }

    /**
     * 已出库结算的批次禁止编辑/删除其关联的巡塘记录，保护历史数据完整性
     */
    private void assertBatchNotHarvested(String batchNo) {
        if (batchNo == null || batchNo.isEmpty()) {
            return; // 未关联批次的巡塘记录无需检查
        }
        PurchaseBatch batch = purchaseBatchMapper.selectOne(
                new LambdaQueryWrapper<PurchaseBatch>().eq(PurchaseBatch::getBatchNo, batchNo));
        if (batch != null && batch.getBatchStatus() != null && batch.getBatchStatus() == 3) {
            throw new BusinessException(400, "关联批次已出库结算，历史巡塘数据不可编辑或删除");
        }
    }
}
