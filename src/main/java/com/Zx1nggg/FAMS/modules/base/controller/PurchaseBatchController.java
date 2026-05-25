package com.Zx1nggg.FAMS.modules.base.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.base.dto.PurchaseBatchDTO;
import com.Zx1nggg.FAMS.modules.base.service.IPurchaseBatchService;
import com.Zx1nggg.FAMS.modules.base.vo.PurchaseBatchVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "采购批次管理")
@RestController
@RequestMapping("/base/purchase-batch")
public class PurchaseBatchController {

    @Autowired
    private IPurchaseBatchService purchaseBatchService;

    @Operation(summary = "分页查询采购批次列表")
    @GetMapping("/list")
    public Result<Page<PurchaseBatchVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Byte batchStatus,
            @RequestParam(required = false) String batchNo) {
        return Result.success(purchaseBatchService.pageQuery(pageNum, pageSize, farmId, batchStatus, batchNo));
    }

    @Operation(summary = "根据ID查询采购批次")
    @GetMapping("/{id}")
    public Result<PurchaseBatchVO> getById(@PathVariable Long id) {
        PurchaseBatchVO vo = purchaseBatchService.queryById(id);
        if (vo == null) {
            return Result.error(404, "采购批次不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "采购批次管理", businessType = 1)
    @Operation(summary = "新增采购批次")
    @PostMapping
    public Result<PurchaseBatchVO> create(@Valid @RequestBody PurchaseBatchDTO dto) {
        return Result.success(purchaseBatchService.create(dto));
    }

    @Log(title = "采购批次管理", businessType = 2)
    @Operation(summary = "修改采购批次")
    @PutMapping("/{id}")
    public Result<PurchaseBatchVO> update(@PathVariable Long id, @Valid @RequestBody PurchaseBatchDTO dto) {
        PurchaseBatchVO vo = purchaseBatchService.update(id, dto);
        if (vo == null) {
            return Result.error(404, "采购批次不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "采购批次管理", businessType = 3)
    @Operation(summary = "批量删除采购批次")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        purchaseBatchService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
