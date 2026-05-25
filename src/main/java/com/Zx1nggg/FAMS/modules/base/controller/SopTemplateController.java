package com.Zx1nggg.FAMS.modules.base.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.base.dto.SopTemplateDTO;
import com.Zx1nggg.FAMS.modules.base.service.ISopTemplateService;
import com.Zx1nggg.FAMS.modules.base.vo.SopTemplateVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "SOP模板管理")
@RestController
@RequestMapping("/base/sop-template")
public class SopTemplateController {

    @Autowired
    private ISopTemplateService sopTemplateService;

    @Operation(summary = "分页查询SOP模板列表")
    @GetMapping("/list")
    public Result<Page<SopTemplateVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String stageName,
            @RequestParam(required = false) String taskType) {
        return Result.success(sopTemplateService.pageQuery(pageNum, pageSize, categoryId, stageName, taskType));
    }

    @Operation(summary = "根据ID查询SOP模板")
    @GetMapping("/{id}")
    public Result<SopTemplateVO> getById(@PathVariable Long id) {
        SopTemplateVO vo = sopTemplateService.queryById(id);
        if (vo == null) {
            return Result.error(404, "SOP模板不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "SOP模板管理", businessType = 1)
    @Operation(summary = "新增SOP模板")
    @PostMapping
    public Result<SopTemplateVO> create(@Valid @RequestBody SopTemplateDTO dto) {
        return Result.success(sopTemplateService.create(dto));
    }

    @Log(title = "SOP模板管理", businessType = 2)
    @Operation(summary = "修改SOP模板")
    @PutMapping("/{id}")
    public Result<SopTemplateVO> update(@PathVariable Long id, @Valid @RequestBody SopTemplateDTO dto) {
        SopTemplateVO vo = sopTemplateService.update(id, dto);
        if (vo == null) {
            return Result.error(404, "SOP模板不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "SOP模板管理", businessType = 3)
    @Operation(summary = "批量删除SOP模板")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        sopTemplateService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
