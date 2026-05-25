package com.Zx1nggg.FAMS.modules.base.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.base.dto.SupplierDTO;
import com.Zx1nggg.FAMS.modules.base.service.ISupplierService;
import com.Zx1nggg.FAMS.modules.base.vo.SupplierVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "供应商管理")
@RestController
@RequestMapping("/base/supplier")
public class SupplierController {

    @Autowired
    private ISupplierService supplierService;

    @Operation(summary = "分页查询供应商列表")
    @GetMapping("/list")
    public Result<Page<SupplierVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String supplierName) {
        return Result.success(supplierService.pageQuery(pageNum, pageSize, supplierName));
    }

    @Operation(summary = "查询全部供应商（下拉列表用）")
    @GetMapping("/all")
    public Result<List<SupplierVO>> all() {
        return Result.success(supplierService.listAll());
    }

    @Operation(summary = "根据ID查询供应商")
    @GetMapping("/{id}")
    public Result<SupplierVO> getById(@PathVariable Long id) {
        SupplierVO vo = supplierService.queryById(id);
        if (vo == null) {
            return Result.error(404, "供应商不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "供应商管理", businessType = 1)
    @Operation(summary = "新增供应商")
    @PostMapping
    public Result<SupplierVO> create(@Valid @RequestBody SupplierDTO dto) {
        return Result.success(supplierService.create(dto));
    }

    @Log(title = "供应商管理", businessType = 2)
    @Operation(summary = "修改供应商")
    @PutMapping("/{id}")
    public Result<SupplierVO> update(@PathVariable Long id, @Valid @RequestBody SupplierDTO dto) {
        SupplierVO vo = supplierService.update(id, dto);
        if (vo == null) {
            return Result.error(404, "供应商不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "供应商管理", businessType = 3)
    @Operation(summary = "批量删除供应商")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        supplierService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
