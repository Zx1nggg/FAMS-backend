package com.Zx1nggg.FAMS.modules.base.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.base.dto.FarmDTO;
import com.Zx1nggg.FAMS.modules.base.service.IFarmService;
import com.Zx1nggg.FAMS.modules.base.vo.FarmVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "养殖场管理")
@RestController
@RequestMapping("/base/farm")
public class FarmController {

    @Autowired
    private IFarmService farmService;

    @Operation(summary = "分页查询养殖场列表")
    @GetMapping("/list")
    public Result<Page<FarmVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String farmName) {
        return Result.success(farmService.pageQuery(pageNum, pageSize, farmName));
    }

    @Operation(summary = "根据ID查询养殖场")
    @GetMapping("/{id}")
    public Result<FarmVO> getById(@PathVariable Long id) {
        FarmVO vo = farmService.queryById(id);
        if (vo == null) {
            return Result.error(404, "养殖场不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "养殖场管理", businessType = 1)
    @Operation(summary = "新增养殖场")
    @PostMapping
    public Result<FarmVO> create(@Valid @RequestBody FarmDTO dto, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("currentUserId");
        if (currentUserId == null) {
            return Result.error(401, "当前用户信息缺失，请重新登录");
        }
        return Result.success(farmService.create(dto, currentUserId));
    }

    @Log(title = "养殖场管理", businessType = 2)
    @Operation(summary = "修改养殖场")
    @PutMapping("/{id}")
    public Result<FarmVO> update(@PathVariable Long id, @Valid @RequestBody FarmDTO dto) {
        FarmVO vo = farmService.update(id, dto);
        if (vo == null) {
            return Result.error(404, "养殖场不存在");
        }
        return Result.success(vo);
    }

    @Log(title = "养殖场管理", businessType = 3)
    @Operation(summary = "批量删除养殖场（软删除，支持恢复）")
    @DeleteMapping("/{ids}")
    public Result<String> delete(@PathVariable List<Long> ids) {
        farmService.batchDelete(ids);
        return Result.success("删除成功");
    }

    @Log(title = "养殖场管理", businessType = 4)
    @Operation(summary = "恢复已删除的养殖场")
    @PutMapping("/restore/{ids}")
    public Result<String> restore(@PathVariable List<Long> ids) {
        farmService.restore(ids);
        return Result.success("恢复成功");
    }
}
