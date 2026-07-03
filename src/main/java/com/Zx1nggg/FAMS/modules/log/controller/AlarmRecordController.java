package com.Zx1nggg.FAMS.modules.log.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.log.dto.AlarmActionDTO;
import com.Zx1nggg.FAMS.modules.log.service.IAlarmRecordService;
import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统告警")
@RestController
@RequestMapping("/log/alarm-record")
public class AlarmRecordController {
    @Autowired private IAlarmRecordService alarmRecordService;

    @Operation(summary = "分页查询告警事件")
    @GetMapping("/list")
    public Result<Page<AlarmRecordVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Long pondId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) Byte severity,
            @RequestParam(required = false) String alarmCode) {
        return Result.success(alarmRecordService.pageQuery(
                pageNum, pageSize, farmId, pondId, activeOnly, status, severity, alarmCode));
    }

    @Log(title = "确认告警", businessType = 2)
    @PutMapping("/{id}/acknowledge")
    public Result<String> acknowledge(@PathVariable Long id, @Valid @RequestBody(required = false) AlarmActionDTO dto) {
        alarmRecordService.acknowledge(id, remark(dto));
        return Result.success("告警已确认");
    }

    @Log(title = "开始处理告警", businessType = 2)
    @PutMapping("/{id}/start-processing")
    public Result<String> startProcessing(@PathVariable Long id, @Valid @RequestBody(required = false) AlarmActionDTO dto) {
        alarmRecordService.startProcessing(id, remark(dto));
        return Result.success("告警已进入处理中");
    }

    @Log(title = "解决告警", businessType = 2)
    @PutMapping("/{id}/resolve")
    public Result<String> resolve(@PathVariable Long id, @Valid @RequestBody(required = false) AlarmActionDTO dto) {
        alarmRecordService.resolve(id, remark(dto));
        return Result.success("告警已解决");
    }

    @Log(title = "关闭告警", businessType = 2)
    @PutMapping("/{id}/close")
    public Result<String> close(@PathVariable Long id, @Valid @RequestBody(required = false) AlarmActionDTO dto) {
        alarmRecordService.close(id, remark(dto));
        return Result.success("告警已关闭");
    }

    @Log(title = "重新打开告警", businessType = 2)
    @PutMapping("/{id}/reopen")
    public Result<String> reopen(@PathVariable Long id, @Valid @RequestBody(required = false) AlarmActionDTO dto) {
        alarmRecordService.reopen(id, remark(dto));
        return Result.success("告警已重新打开");
    }

    private String remark(AlarmActionDTO dto) {
        return dto == null ? null : dto.getRemark();
    }
}