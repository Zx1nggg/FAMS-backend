package com.Zx1nggg.FAMS.modules.log.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.log.dto.PondFeedLogDTO;
import com.Zx1nggg.FAMS.modules.log.service.IPondFeedLogService;
import com.Zx1nggg.FAMS.modules.log.vo.PondFeedLogVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "投喂换水日志")
@RestController
@RequestMapping("/log/pond-feed-log")
public class PondFeedLogController {

    @Autowired
    private IPondFeedLogService pondFeedLogService;

    @Operation(summary = "分页查询投喂换水日志")
    @GetMapping("/list")
    public Result<Page<PondFeedLogVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long pondId,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Long patrolLogId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(pondFeedLogService.pageQuery(pageNum, pageSize,
                pondId, farmId, patrolLogId, startDate, endDate));
    }

    @Operation(summary = "根据ID查询投喂换水日志")
    @GetMapping("/{id}")
    public Result<PondFeedLogVO> getById(@PathVariable Long id) {
        PondFeedLogVO vo = pondFeedLogService.queryById(id);
        if (vo == null) return Result.error(404, "记录不存在");
        return Result.success(vo);
    }

    @Log(title = "投喂换水日志", businessType = 1)
    @Operation(summary = "新增投喂换水记录")
    @PostMapping
    public Result<PondFeedLogVO> create(@Valid @RequestBody PondFeedLogDTO dto) {
        return Result.success(pondFeedLogService.create(dto));
    }

    @Log(title = "投喂换水日志", businessType = 2)
    @Operation(summary = "修改投喂换水记录")
    @PutMapping("/{id}")
    public Result<PondFeedLogVO> update(@PathVariable Long id, @Valid @RequestBody PondFeedLogDTO dto) {
        PondFeedLogVO vo = pondFeedLogService.update(id, dto);
        if (vo == null) return Result.error(404, "记录不存在");
        return Result.success(vo);
    }

    @Log(title = "投喂换水日志", businessType = 3)
    @Operation(summary = "批量删除投喂换水记录")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        pondFeedLogService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
