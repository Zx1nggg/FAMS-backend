package com.Zx1nggg.FAMS.modules.system.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.system.dto.UpdateUserProfileDTO;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import com.Zx1nggg.FAMS.modules.system.vo.UserProfileVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Tag(name = "用户个人主页")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @Value("${app.upload.avatar-dir:uploads/avatar}")
    private String avatarDir;

    @Operation(summary = "获取当前用户个人资料")
    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfileVO vo = userService.getProfile(userId);
        if (vo == null) return Result.error(404, "用户不存在");
        return Result.success(vo);
    }

    @Log(title = "用户资料", businessType = 2)
    @Operation(summary = "更新当前用户个人资料")
    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@RequestBody UpdateUserProfileDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfileVO vo = userService.updateProfile(userId, dto);
        if (vo == null) return Result.error(404, "用户不存在");
        return Result.success(vo);
    }

    @Log(title = "用户头像", businessType = 2)
    @Operation(summary = "上传用户头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的头像文件");
        }

        // 仅允许图片格式
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "仅支持上传图片文件");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(avatarDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名：userId_uuid.扩展名
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            // 保存文件
            File dest = uploadPath.resolve(fileName).toFile();
            file.transferTo(dest);

            // 存入数据库的相对路径（相对于资源映射的根目录）
            String avatarPath = "uploads/avatar/" + fileName;
            userService.updateAvatar(userId, avatarPath);

            return Result.success(avatarPath);
        } catch (IOException e) {
            return Result.error(500, "头像上传失败: " + e.getMessage());
        }
    }
}
