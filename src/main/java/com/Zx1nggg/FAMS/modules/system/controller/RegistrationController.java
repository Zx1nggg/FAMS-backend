package com.Zx1nggg.FAMS.modules.system.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.system.dto.ApprovalReqDTO;
import com.Zx1nggg.FAMS.modules.system.dto.RegistrationReqDTO;
import com.Zx1nggg.FAMS.modules.system.service.IRegistrationApplicationService;
import com.Zx1nggg.FAMS.modules.system.vo.RegistrationApplicationVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "入驻申请与审批")
@RestController
public class RegistrationController {

    @Autowired
    private IRegistrationApplicationService registrationApplicationService;

    // ==================== 公开接口 ====================

    @Operation(summary = "提交入驻申请")
    @PostMapping("/auth/register")
    public Result<String> register(@Valid @RequestBody RegistrationReqDTO dto) {
        registrationApplicationService.submitApplication(dto);
        return Result.success("入驻申请已提交，请等待管理员审核");
    }

    @Operation(summary = "检查用户名是否可用")
    @GetMapping("/auth/check-username")
    public Result<Map<String, Object>> checkUsername(@RequestParam String username) {
        boolean available = registrationApplicationService.isUsernameAvailable(username);
        Map<String, Object> data = Map.of(
                "username", username,
                "available", available
        );
        return Result.success(data);
    }

    @Operation(summary = "查询入驻申请状态（申请人查询）")
    @GetMapping("/auth/registration-status")
    public Result<RegistrationApplicationVO> queryStatus(@RequestParam String username) {
        RegistrationApplicationVO vo = registrationApplicationService.queryStatusByUsername(username);
        return Result.success(vo);
    }

    // ==================== 管理员接口 ====================

    @Operation(summary = "获取入驻申请列表（管理员）")
    @GetMapping("/admin/registrations")
    public Result<Page<RegistrationApplicationVO>> listApplications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Page<RegistrationApplicationVO> page = registrationApplicationService.listApplications(pageNum, pageSize, status);
        return Result.success(page);
    }

    @Operation(summary = "获取入驻申请详情（管理员）")
    @GetMapping("/admin/registrations/{id}")
    public Result<RegistrationApplicationVO> getApplicationDetail(@PathVariable Long id) {
        RegistrationApplicationVO vo = registrationApplicationService.getApplicationDetail(id);
        return Result.success(vo);
    }

    @Log(title = "入驻审批", businessType = 2)
    @Operation(summary = "审批入驻申请（通过或拒绝）")
    @PutMapping("/admin/registrations/{id}/approve")
    public Result<Map<String, Object>> approveApplication(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalReqDTO dto) {
        Long reviewerId = SecurityUtils.getCurrentUserId();
        Long newUserId = registrationApplicationService.approveApplication(id, reviewerId, dto);

        String msg = dto.getStatus() == 1 ? "审批通过，已创建用户账号" : "已拒绝该入驻申请";
        Map<String, Object> data = Map.of(
                "status", dto.getStatus(),
                "newUserId", newUserId != null ? newUserId : 0
        );
        return Result.success(data);
    }
}
