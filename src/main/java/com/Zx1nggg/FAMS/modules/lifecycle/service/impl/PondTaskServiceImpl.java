package com.Zx1nggg.FAMS.modules.lifecycle.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.entity.SopTemplate;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.base.mapper.SopTemplateMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.entity.PondTask;
import com.Zx1nggg.FAMS.modules.lifecycle.mapper.PondTaskMapper;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IPondTaskService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.PondTaskVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PondTaskServiceImpl extends ServiceImpl<PondTaskMapper, PondTask> implements IPondTaskService {

    @Resource
    private PondMapper pondMapper;

    @Resource
    private SopTemplateMapper sopTemplateMapper;

    @Override
    public Page<PondTaskVO> pageQuery(Integer pageNum, Integer pageSize,
                                      Long pondId, Long farmId, LocalDate scheduledDate,
                                      Byte status, String batchNo) {
        // 🌟 数据隔离：FARMER 只能查看本农场的任务
        if (SecurityUtils.isFarmer()) {
            farmId = SecurityUtils.getCurrentFarmId();
        }
        LambdaQueryWrapper<PondTask> wrapper = new LambdaQueryWrapper<>();

        if (farmId != null) {
            List<Pond> ponds = pondMapper.selectList(
                    new LambdaQueryWrapper<Pond>().eq(Pond::getFarmId, farmId));
            List<Long> pondIds = ponds.stream().map(Pond::getId).toList();
            if (pondIds.isEmpty()) {
                Page<PondTaskVO> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            wrapper.in(PondTask::getPondId, pondIds);
        }
        if (pondId != null) {
            wrapper.eq(PondTask::getPondId, pondId);
        }
        if (scheduledDate != null) {
            wrapper.eq(PondTask::getScheduledDate, scheduledDate);
        }
        if (status != null) {
            wrapper.eq(PondTask::getStatus, status);
        }
        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.eq(PondTask::getBatchNo, batchNo);
        }
        wrapper.orderByAsc(PondTask::getScheduledDate).orderByAsc(PondTask::getStatus);
        Page<PondTask> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public PondTaskVO queryById(Long id) {
        PondTask task = getById(id);
        if (task == null) return null;
        // 🌟 数据隔离
        checkFarmAccessByPondId(task.getPondId());
        return toVO(task);
    }

    @Override
    public void checkOff(Long id) {
        PondTask task = getById(id);
        if (task == null) throw new BusinessException(404, "任务不存在");
        // 🌟 数据隔离
        checkFarmAccessByPondId(task.getPondId());
        if (task.getStatus() != null && task.getStatus() == 1) {
            throw new BusinessException(400, "任务已完成，无需重复打卡");
        }
        task.setStatus((byte) 1);
        task.setFinishTime(LocalDateTime.now());
        task.setOperatorId(SecurityUtils.getCurrentUserId());
        updateById(task);
    }

    @Override
    public void batchCheckOff(List<Long> ids) {
        for (Long id : ids) {
            checkOff(id);
        }
    }

    @Override
    public void batchDelete(List<Long> ids) {
        // 🌟 数据隔离：FARMER 只能删除本农场任务的记录
        if (SecurityUtils.isFarmer()) {
            List<PondTask> tasks = listByIds(ids);
            for (PondTask task : tasks) {
                checkFarmAccessByPondId(task.getPondId());
            }
        }
        removeByIds(ids);
    }

    @Override
    public void generateTasks(Long batchId, Long pondId, String batchNo, Long seedlingId, LocalDate stockingDate) {
        // 根据苗种ID查询所有匹配的SOP模板
        List<SopTemplate> templates = sopTemplateMapper.selectList(
                new LambdaQueryWrapper<SopTemplate>().eq(SopTemplate::getCategoryId, seedlingId));

        if (templates.isEmpty()) return;

        List<PondTask> tasks = new ArrayList<>();
        for (SopTemplate tpl : templates) {
            PondTask task = new PondTask();
            task.setPondId(pondId);
            task.setBatchNo(batchNo);
            task.setTaskType(tpl.getTaskType());
            task.setTaskDesc(tpl.getTaskDesc());
            task.setScheduledDate(stockingDate.plusDays(tpl.getDayOffset()));
            task.setStatus((byte) 0);
            tasks.add(task);
        }
        saveBatch(tasks);
    }

    // ==================== private helpers ====================

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

    private PondTaskVO toVO(PondTask task) {
        PondTaskVO vo = new PondTaskVO();
        BeanUtils.copyProperties(task, vo);

        if (task.getPondId() != null) {
            Pond pond = pondMapper.selectById(task.getPondId());
            if (pond != null) vo.setPondName(pond.getPondName());
        }
        return vo;
    }

    private Page<PondTaskVO> toVOPage(Page<PondTask> page) {
        Page<PondTaskVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
