package com.Zx1nggg.FAMS.modules.log.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.log.service.IAlarmRecordService;
import com.Zx1nggg.FAMS.modules.log.vo.AlarmRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统告警")
@RestController
@RequestMapping("/log/alarm-record")
public class AlarmRecordController {

    @Autowired
    private IAlarmRecordService alarmRecordService;

    @Operation(summary = "分页查询告警记录")
    @GetMapping("/list")
    public Result<Page<AlarmRecordVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Byte isHandled,
            @RequestParam(required = false) String alarmType) {
        return Result.success(alarmRecordService.pageQuery(pageNum, pageSize,
                farmId, isHandled, alarmType));
    }

    @Log(title = "告警处理", businessType = 2)
    @Operation(summary = "标记告警为已处理")
    @PutMapping("/{id}/handle")
    public Result<String> handle(@PathVariable Long id) {
        alarmRecordService.handleAlarm(id);
        return Result.success("告警已标记为处理");
    }
}
