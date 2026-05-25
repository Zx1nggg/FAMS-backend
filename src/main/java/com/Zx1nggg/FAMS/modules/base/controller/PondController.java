package com.Zx1nggg.FAMS.modules.base.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.base.dto.PondDTO;
import com.Zx1nggg.FAMS.modules.base.service.IPondService;
import com.Zx1nggg.FAMS.modules.base.vo.PondVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "池塘管理")
@RestController
@RequestMapping("/base/pond")
public class PondController {

    @Autowired
    private IPondService pondService;

    @Operation(summary = "分页查询池塘列表")
    @GetMapping("/list")
    public Result<Page<PondVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) String pondName) {
        return Result.success(pondService.pageQuery(pageNum, pageSize, farmId, pondName));
    }

    @Operation(summary = "根据ID查询池塘")
    @GetMapping("/{id}")
    public Result<PondVO> getById(@PathVariable Long id) {
        PondVO vo = pondService.queryById(id);
        if (vo == null) {
            return Result.error(404, "池塘不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "池塘管理", businessType = 1)
    @Operation(summary = "新增池塘")
    @PostMapping
    public Result<PondVO> create(@Valid @RequestBody PondDTO dto) {
        return Result.success(pondService.create(dto));
    }

    @Log(title = "池塘管理", businessType = 2)
    @Operation(summary = "修改池塘")
    @PutMapping("/{id}")
    public Result<PondVO> update(@PathVariable Long id, @Valid @RequestBody PondDTO dto) {
        PondVO vo = pondService.update(id, dto);
        if (vo == null) {
            return Result.error(404, "池塘不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "池塘管理", businessType = 3)
    @Operation(summary = "批量删除池塘")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        pondService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
