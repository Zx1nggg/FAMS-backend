package com.Zx1nggg.FAMS.modules.lifecycle.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.lifecycle.dto.BatchGrowthLogDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IBatchGrowthLogService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.BatchGrowthLogVO;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.GrowthChartVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "生长死亡抽测记录")
@RestController
@RequestMapping("/lifecycle/batch-growth-log")
public class BatchGrowthLogController {

    @Autowired
    private IBatchGrowthLogService batchGrowthLogService;

    @Operation(summary = "分页查询生长死亡抽测记录")
    @GetMapping("/list")
    public Result<Page<BatchGrowthLogVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) Long pondId,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Long patrolLogId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(batchGrowthLogService.pageQuery(pageNum, pageSize,
                batchNo, pondId, farmId, patrolLogId, startDate, endDate));
    }

    @Operation(summary = "根据ID查询生长死亡记录")
    @GetMapping("/{id}")
    public Result<BatchGrowthLogVO> getById(@PathVariable Long id) {
        BatchGrowthLogVO vo = batchGrowthLogService.queryById(id);
        if (vo == null) return Result.error(404, "记录不存在");
        return Result.success(vo);
    }

    @Log(title = "生长死亡记录", businessType = 1)
    @Operation(summary = "新增生长死亡抽测记录")
    @PostMapping
    public Result<BatchGrowthLogVO> create(@Valid @RequestBody BatchGrowthLogDTO dto) {
        return Result.success(batchGrowthLogService.create(dto));
    }

    @Log(title = "生长死亡记录", businessType = 2)
    @Operation(summary = "修改生长死亡抽测记录")
    @PutMapping("/{id}")
    public Result<BatchGrowthLogVO> update(@PathVariable Long id, @Valid @RequestBody BatchGrowthLogDTO dto) {
        BatchGrowthLogVO vo = batchGrowthLogService.update(id, dto);
        if (vo == null) return Result.error(404, "记录不存在");
        return Result.success(vo);
    }

    @Log(title = "生长死亡记录", businessType = 3)
    @Operation(summary = "批量删除生长死亡记录")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        batchGrowthLogService.batchDelete(ids);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取生长曲线图表数据")
    @GetMapping("/growth-chart")
    public Result<GrowthChartVO> growthChart(@RequestParam String batchNo,
                                             @RequestParam Long pondId) {
        GrowthChartVO vo = batchGrowthLogService.getGrowthChart(batchNo, pondId);
        if (vo == null) {
            return Result.error(404, "批次不存在");
        }
        return Result.success(vo);
    }
}
