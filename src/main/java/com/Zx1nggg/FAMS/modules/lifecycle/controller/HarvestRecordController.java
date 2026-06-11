package com.Zx1nggg.FAMS.modules.lifecycle.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.lifecycle.dto.HarvestRecordDTO;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IHarvestRecordService;
import com.Zx1nggg.FAMS.modules.lifecycle.vo.HarvestRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 出塘结算与消费者防伪溯源凭证表 前端控制器
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Tag(name = "出塘结算管理")
@RestController
@RequestMapping("/lifecycle/harvest-record")
public class HarvestRecordController {

    @Autowired
    private IHarvestRecordService harvestRecordService;

    @Operation(summary = "分页查询出塘结算记录")
    @GetMapping("/list")
    public Result<Page<HarvestRecordVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) Long pondId) {
        return Result.success(harvestRecordService.pageQuery(pageNum, pageSize, farmId, batchNo, pondId));
    }

    @Operation(summary = "根据ID查询出塘结算详情")
    @GetMapping("/{id}")
    public Result<HarvestRecordVO> getById(@PathVariable Long id) {
        HarvestRecordVO vo = harvestRecordService.queryById(id);
        if (vo == null) {
            return Result.error(404, "出塘结算记录不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "出塘结算管理", businessType = 1)
    @Operation(summary = "新增出塘结算记录（自动计算金额 + 批次完结）")
    @PostMapping
    public Result<HarvestRecordVO> create(@Valid @RequestBody HarvestRecordDTO dto) {
        return Result.success(harvestRecordService.create(dto));
    }

    @Log(title = "出塘结算管理", businessType = 2)
    @Operation(summary = "修改出塘结算记录（重算金额）")
    @PutMapping("/{id}")
    public Result<HarvestRecordVO> update(@PathVariable Long id, @Valid @RequestBody HarvestRecordDTO dto) {
        return Result.success(harvestRecordService.update(id, dto));
    }

    @Log(title = "出塘结算管理", businessType = 3)
    @Operation(summary = "批量软删除出塘结算记录（恢复批次状态为养殖中）")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        harvestRecordService.batchDelete(ids);
        return Result.success("删除成功");
    }

    @Operation(summary = "结算预览：获取批次的养殖汇总数据")
    @GetMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestParam Long batchId) {
        return Result.success(harvestRecordService.preview(batchId));
    }
}
