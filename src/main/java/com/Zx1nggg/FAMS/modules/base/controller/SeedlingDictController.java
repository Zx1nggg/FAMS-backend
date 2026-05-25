package com.Zx1nggg.FAMS.modules.base.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.base.dto.SeedlingDictDTO;
import com.Zx1nggg.FAMS.modules.base.service.ISeedlingDictService;
import com.Zx1nggg.FAMS.modules.base.vo.SeedlingDictVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "苗种字典管理")
@RestController
@RequestMapping("/base/seedling-dict")
public class SeedlingDictController {

    @Autowired
    private ISeedlingDictService seedlingDictService;

    @Operation(summary = "分页查询苗种字典列表")
    @GetMapping("/list")
    public Result<Page<SeedlingDictVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String categoryName) {
        return Result.success(seedlingDictService.pageQuery(pageNum, pageSize, categoryName));
    }

    @Operation(summary = "查询全部苗种字典（下拉列表用）")
    @GetMapping("/all")
    public Result<List<SeedlingDictVO>> all() {
        return Result.success(seedlingDictService.listAll());
    }

    @Operation(summary = "根据ID查询苗种字典")
    @GetMapping("/{id}")
    public Result<SeedlingDictVO> getById(@PathVariable Long id) {
        SeedlingDictVO vo = seedlingDictService.queryById(id);
        if (vo == null) {
            return Result.error(404, "苗种字典不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "苗种字典管理", businessType = 1)
    @Operation(summary = "新增苗种字典")
    @PostMapping
    public Result<SeedlingDictVO> create(@Valid @RequestBody SeedlingDictDTO dto) {
        return Result.success(seedlingDictService.create(dto));
    }

    @Log(title = "苗种字典管理", businessType = 2)
    @Operation(summary = "修改苗种字典")
    @PutMapping("/{id}")
    public Result<SeedlingDictVO> update(@PathVariable Long id, @Valid @RequestBody SeedlingDictDTO dto) {
        SeedlingDictVO vo = seedlingDictService.update(id, dto);
        if (vo == null) {
            return Result.error(404, "苗种字典不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "苗种字典管理", businessType = 3)
    @Operation(summary = "批量删除苗种字典")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        seedlingDictService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
