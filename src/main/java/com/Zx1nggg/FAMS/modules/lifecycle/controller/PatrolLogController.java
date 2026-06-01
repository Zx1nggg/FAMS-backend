package com.Zx1nggg.FAMS.modules.lifecycle.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.lifecycle.dto.PatrolLogDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IPatrolLogService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.PatrolLogVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "日常巡塘台账")
@RestController
@RequestMapping("/lifecycle/patrol-log")
public class PatrolLogController {

    @Autowired
    private IPatrolLogService patrolLogService;

    @Operation(summary = "分页查询巡塘台账列表")
    @GetMapping("/list")
    public Result<Page<PatrolLogVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long pondId,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(patrolLogService.pageQuery(pageNum, pageSize,
                pondId, farmId, startDate, endDate));
    }

    @Operation(summary = "根据ID查询巡塘台账")
    @GetMapping("/{id}")
    public Result<PatrolLogVO> getById(@PathVariable Long id) {
        PatrolLogVO vo = patrolLogService.queryById(id);
        if (vo == null) return Result.error(404, "巡塘记录不存在");
        return Result.success(vo);
    }

    @Log(title = "巡塘台账管理", businessType = 1)
    @Operation(summary = "开始巡塘（新增巡塘台账）")
    @PostMapping
    public Result<PatrolLogVO> create(@Valid @RequestBody PatrolLogDTO dto) {
        return Result.success(patrolLogService.create(dto));
    }

    @Log(title = "巡塘台账管理", businessType = 2)
    @Operation(summary = "修改巡塘台账")
    @PutMapping("/{id}")
    public Result<PatrolLogVO> update(@PathVariable Long id, @Valid @RequestBody PatrolLogDTO dto) {
        PatrolLogVO vo = patrolLogService.update(id, dto);
        if (vo == null) return Result.error(404, "巡塘记录不存在");
        return Result.success(vo);
    }

    @Log(title = "巡塘台账管理", businessType = 3)
    @Operation(summary = "批量删除巡塘台账")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        patrolLogService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
