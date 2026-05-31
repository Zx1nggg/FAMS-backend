package com.Zx1nggg.FAMS.modules.base.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.base.dto.StockingDTO;
import com.Zx1nggg.FAMS.modules.base.service.IStockingService;
import com.Zx1nggg.FAMS.modules.base.vo.StockingVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "投放登记管理")
@RestController
@RequestMapping("/base/stocking")
public class StockingController {

    @Autowired
    private IStockingService stockingService;

    @Operation(summary = "分页查询投放登记列表")
    @GetMapping("/list")
    public Result<Page<StockingVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Long pondId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(stockingService.pageQuery(pageNum, pageSize,
                farmId, pondId, batchId, startDate, endDate));
    }

    @Operation(summary = "根据ID查询投放登记")
    @GetMapping("/{id}")
    public Result<StockingVO> getById(@PathVariable Long id) {
        StockingVO vo = stockingService.queryById(id);
        if (vo == null) {
            return Result.error(404, "投放登记不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "投放登记管理", businessType = 1)
    @Operation(summary = "新增投放登记")
    @PostMapping
    public Result<StockingVO> create(@Valid @RequestBody StockingDTO dto) {
        return Result.success(stockingService.create(dto));
    }

    @Log(title = "投放登记管理", businessType = 2)
    @Operation(summary = "修改投放登记")
    @PutMapping("/{id}")
    public Result<StockingVO> update(@PathVariable Long id, @Valid @RequestBody StockingDTO dto) {
        StockingVO vo = stockingService.update(id, dto);
        if (vo == null) {
            return Result.error(404, "投放登记不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "投放登记管理", businessType = 3)
    @Operation(summary = "批量删除投放登记")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        stockingService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
