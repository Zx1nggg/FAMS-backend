package com.Zx1nggg.FAMS.modules.lifecycle.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IPondTaskService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.PondTaskVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "SOP待办任务")
@RestController
@RequestMapping("/lifecycle/pond-task")
public class PondTaskController {

    @Autowired
    private IPondTaskService pondTaskService;

    @Operation(summary = "分页查询待办任务列表")
    @GetMapping("/list")
    public Result<Page<PondTaskVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long pondId,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) LocalDate scheduledDate,
            @RequestParam(required = false) Byte status,
            @RequestParam(required = false) String batchNo) {
        return Result.success(pondTaskService.pageQuery(pageNum, pageSize,
                pondId, farmId, scheduledDate, status, batchNo));
    }

    @Operation(summary = "根据ID查询任务")
    @GetMapping("/{id}")
    public Result<PondTaskVO> getById(@PathVariable Long id) {
        PondTaskVO vo = pondTaskService.queryById(id);
        if (vo == null) return Result.error(404, "任务不存在");
        return Result.success(vo);
    }

    @Log(title = "SOP任务管理", businessType = 2)
    @Operation(summary = "任务打卡（标记完成）")
    @PutMapping("/{id}/check-off")
    public Result<String> checkOff(@PathVariable Long id) {
        pondTaskService.checkOff(id);
        return Result.success("打卡成功");
    }

    @Log(title = "SOP任务管理", businessType = 2)
    @Operation(summary = "批量打卡")
    @PutMapping("/batch-check-off")
    public Result<String> batchCheckOff(@RequestBody List<Long> ids) {
        pondTaskService.batchCheckOff(ids);
        return Result.success("批量打卡成功");
    }

    @Log(title = "SOP任务管理", businessType = 3)
    @Operation(summary = "批量删除任务")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        pondTaskService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
