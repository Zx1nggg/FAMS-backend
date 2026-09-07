package com.Zx1nggg.FAMS.modules.regulator.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.*;
import com.Zx1nggg.FAMS.modules.base.mapper.*;
import com.Zx1nggg.FAMS.modules.regulator.dto.*;
import com.Zx1nggg.FAMS.modules.regulator.entity.InspectionRecord;
import com.Zx1nggg.FAMS.modules.regulator.mapper.InspectionRecordMapper;
import com.Zx1nggg.FAMS.modules.regulator.service.IInspectionRecordService;
import com.Zx1nggg.FAMS.modules.regulator.vo.InspectionRecordVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InspectionRecordServiceImpl extends ServiceImpl<InspectionRecordMapper, InspectionRecord> implements IInspectionRecordService {
    @Autowired private FarmMapper farmMapper;
    @Autowired private PondMapper pondMapper;
    @Autowired private ObjectMapper objectMapper;

    private void requireRegulator() {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isRegulator()) throw new BusinessException(403, "仅监管方或管理员可操作抽检档案");
    }
    public Page<InspectionRecordVO> pageQuery(int pageNum, int pageSize, Long farmId, String result, String rectifyStatus, LocalDate startDate, LocalDate endDate) {
        requireRegulator();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) throw new BusinessException(400, "日期范围不合法");
        var wrapper = new LambdaQueryWrapper<InspectionRecord>();
        if (farmId != null) wrapper.eq(InspectionRecord::getFarmId, farmId);
        if (result != null && !result.isBlank()) wrapper.eq(InspectionRecord::getResult, result);
        if (rectifyStatus != null && !rectifyStatus.isBlank()) wrapper.eq(InspectionRecord::getRectifyStatus, rectifyStatus);
        if (startDate != null) wrapper.ge(InspectionRecord::getInspectionDate, startDate);
        if (endDate != null) wrapper.le(InspectionRecord::getInspectionDate, endDate);
        wrapper.orderByDesc(InspectionRecord::getInspectionDate).orderByDesc(InspectionRecord::getId);
        var page = page(new Page<>(Math.max(1, pageNum), Math.min(200, Math.max(1, pageSize))), wrapper);
        Page<InspectionRecordVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return resultPage;
    }
    public InspectionRecordVO detail(Long id) { requireRegulator(); return toVO(requireRecord(id)); }
    @Transactional
    public InspectionRecordVO create(InspectionRecordDTO dto) {
        requireRegulator(); validate(dto);
        InspectionRecord record = new InspectionRecord(); BeanUtils.copyProperties(dto, record);
        record.setInspectorId(SecurityUtils.getCurrentUserId());
        record.setRectifyStatus("unqualified".equals(dto.getResult()) ? "pending" : "none");
        record.setCreatedAt(LocalDateTime.now()); record.setUpdatedAt(LocalDateTime.now()); save(record);
        return toVO(record);
    }
    @Transactional
    public InspectionRecordVO updateRecord(Long id, InspectionRecordDTO dto) {
        requireRegulator(); validate(dto); InspectionRecord record = requireRecord(id);
        if (!"none".equals(record.getRectifyStatus()) && !"pending".equals(record.getRectifyStatus())) {
            throw new BusinessException(400, "整改开始后不可修改原始抽检档案，请使用整改流程");
        }
        String previous = record.getRectifyStatus();
        BeanUtils.copyProperties(dto, record); record.setId(id);
        record.setRectifyStatus("unqualified".equals(dto.getResult()) ? "pending" : "none"); record.setUpdatedAt(LocalDateTime.now());
        var update = new LambdaUpdateWrapper<InspectionRecord>().eq(InspectionRecord::getId, id)
                .eq(InspectionRecord::getRectifyStatus, previous).set(InspectionRecord::getPondId, dto.getPondId())
                .set(InspectionRecord::getUnqualifiedReason, "qualified".equals(dto.getResult()) ? null : dto.getUnqualifiedReason())
                .set(InspectionRecord::getAttachmentUrls, dto.getAttachmentUrls());
        record.setPondId(null); record.setUnqualifiedReason(null); record.setAttachmentUrls(null);
        if (baseMapper.update(record, update) != 1) throw new BusinessException(409, "档案状态已变化，请刷新重试");
        return detail(id);
    }
    @Transactional
    public void deleteRecords(List<Long> ids) {
        requireRegulator(); if (ids == null || ids.isEmpty()) throw new BusinessException(400, "请选择抽检档案"); removeByIds(ids);
    }
    @Transactional
    public void rectify(Long id, InspectionRectifyDTO dto) {
        requireRegulator(); InspectionRecord record = requireRecord(id);
        Map<String, String> next = Map.of("none", "pending", "pending", "rectifying", "rectifying", "rectified", "rectified", "accepted");
        if (!Objects.equals(next.get(record.getRectifyStatus()), dto.getRectifyStatus())) throw new BusinessException(400, "整改状态必须按下发、整改中、已整改、已验收顺序推进");
        if (dto.getRectifyDeadline() != null && dto.getRectifyDeadline().isBefore(record.getInspectionDate())) throw new BusinessException(400, "整改截止日期不能早于抽检日期");
        if (dto.getRectifyRemark() == null || dto.getRectifyRemark().isBlank()) throw new BusinessException(400, "请填写整改说明");
        var update = new LambdaUpdateWrapper<InspectionRecord>().eq(InspectionRecord::getId, id)
                .eq(InspectionRecord::getRectifyStatus, record.getRectifyStatus())
                .set(InspectionRecord::getRectifyStatus, dto.getRectifyStatus()).set(InspectionRecord::getRectifyRemark, dto.getRectifyRemark())
                .set(InspectionRecord::getUpdatedAt, LocalDateTime.now());
        if (dto.getRectifyDeadline() != null) update.set(InspectionRecord::getRectifyDeadline, dto.getRectifyDeadline());
        if (baseMapper.update(null, update) != 1) throw new BusinessException(409, "整改状态已变化，请刷新重试");
    }
    public Map<String, Object> stats() {
        requireRegulator(); LocalDate start = LocalDate.now().withDayOfMonth(1);
        long total = count(new LambdaQueryWrapper<InspectionRecord>().ge(InspectionRecord::getInspectionDate, start).lt(InspectionRecord::getInspectionDate, start.plusMonths(1)));
        long qualified = count(new LambdaQueryWrapper<InspectionRecord>().ge(InspectionRecord::getInspectionDate, start).lt(InspectionRecord::getInspectionDate, start.plusMonths(1)).eq(InspectionRecord::getResult, "qualified"));
        return Map.of("totalThisMonth", total, "qualifiedRate", total == 0 ? 0 : Math.round(1000.0 * qualified / total) / 10.0,
                "pendingRectify", count(new LambdaQueryWrapper<InspectionRecord>().in(InspectionRecord::getRectifyStatus, "pending", "rectifying")),
                "completedRectify", count(new LambdaQueryWrapper<InspectionRecord>().in(InspectionRecord::getRectifyStatus, "rectified", "accepted")));
    }
    private void validate(InspectionRecordDTO dto) {
        Farm farm = dto.getFarmId() == null ? null : farmMapper.selectById(dto.getFarmId());
        if (farm == null) throw new BusinessException(404, "养殖场不存在或已删除");
        if (dto.getPondId() != null) {
            Pond pond = pondMapper.selectById(dto.getPondId());
            if (pond == null || !Objects.equals(pond.getFarmId(), dto.getFarmId())) throw new BusinessException(400, "池塘不属于所选养殖场");
        }
        if ("unqualified".equals(dto.getResult()) && (dto.getUnqualifiedReason() == null || dto.getUnqualifiedReason().isBlank())) throw new BusinessException(400, "不合格时必须填写原因");
        if (dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isBlank()) {
            try {
                var urls = objectMapper.readTree(dto.getAttachmentUrls());
                if (!urls.isArray() || urls.size() > 10) throw new IllegalArgumentException();
                for (var url : urls) if (!url.isTextual() || !url.asText().matches("/api/regulator/inspections/attachments/[a-f0-9-]+\\.(png|jpg|pdf)")) throw new IllegalArgumentException();
            } catch (Exception e) { throw new BusinessException(400, "附件必须使用本系统上传的文件，最多 10 个"); }
        }
    }
    private InspectionRecord requireRecord(Long id) {
        InspectionRecord record = getById(id); if (record == null) throw new BusinessException(404, "抽检档案不存在"); return record;
    }
    private InspectionRecordVO toVO(InspectionRecord record) {
        InspectionRecordVO vo = new InspectionRecordVO(); BeanUtils.copyProperties(record, vo);
        Farm farm = farmMapper.selectById(record.getFarmId()); if (farm != null) vo.setFarmName(farm.getFarmName());
        if (record.getPondId() != null) { Pond pond = pondMapper.selectById(record.getPondId()); if (pond != null) vo.setPondName(pond.getPondName()); }
        return vo;
    }
}
