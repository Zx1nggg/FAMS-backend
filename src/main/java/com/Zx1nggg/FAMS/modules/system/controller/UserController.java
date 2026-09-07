package com.Zx1nggg.FAMS.modules.system.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.system.dto.UpdateUserProfileDTO;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import com.Zx1nggg.FAMS.modules.system.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.List;
import java.util.UUID;

@Tag(name = "用户个人主页")
@RestController
@RequestMapping("/user")
public class UserController {
    @PutMapping("/password")
    public Result<Void> changePassword(@jakarta.validation.Valid @RequestBody com.Zx1nggg.FAMS.modules.system.dto.ChangePasswordDTO dto,
                                      jakarta.servlet.http.HttpServletResponse response) {
        userService.changePassword(SecurityUtils.getCurrentUserId(), dto);
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("aqua_token", "");
        cookie.setHttpOnly(true); cookie.setPath("/"); cookie.setMaxAge(0); cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return Result.success();
    }

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
    public Result<UserProfileVO> updateProfile(@jakarta.validation.Valid @RequestBody UpdateUserProfileDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfileVO vo = userService.updateProfile(userId, dto);
        if (vo == null) return Result.error(404, "用户不存在");
        return Result.success(vo);
    }

    @Operation(summary = "分页查询用户列表（管理员）")
    @GetMapping("/list")
    public Result<Page<UserProfileVO>> listUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Byte status) {
        return Result.success(userService.pageUsers(pageNum, pageSize, keyword, userType, status));
    }

    @Log(title = "用户账号运维", businessType = 2)
    @Operation(summary = "启用或停用用户账号（管理员）")
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Byte status) {
        userService.updateUserStatus(id, status);
        return Result.success("账号状态已更新");
    }

    @Log(title = "用户账号运维", businessType = 3)
    @Operation(summary = "删除用户账号（管理员）")
    @DeleteMapping("/{ids}")
    public Result<String> deleteUsers(@PathVariable List<Long> ids) {
        userService.deleteUsers(ids);
        return Result.success("删除成功");
    }

    @Log(title = "用户头像", businessType = 2)
    @Operation(summary = "上传用户头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的头像文件");
        }

        if (file.getSize() > 5 * 1024 * 1024) return Result.error(400, "头像大小不能超过 5 MB");
        Long userId = SecurityUtils.getCurrentUserId();
        try (var input = javax.imageio.ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = javax.imageio.ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return Result.error(400, "仅支持有效的 PNG、JPEG 或 GIF 图片");
            var reader = readers.next();
            java.awt.image.BufferedImage image;
            try {
                reader.setInput(input);
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!java.util.Set.of("png", "jpeg", "gif").contains(format)
                        || reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096) {
                    return Result.error(400, "头像格式不支持或尺寸超过 4096 像素");
                }
                image = reader.read(0);
            } finally { reader.dispose(); }
            Path uploadPath = Paths.get(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            String fileName = userId + "_" + UUID.randomUUID() + ".png";
            File dest = uploadPath.resolve(fileName).toFile();
            if (!javax.imageio.ImageIO.write(image, "png", dest)) throw new IOException("Image encoder unavailable");

            // 存入数据库的相对路径（相对于资源映射的根目录）
            String avatarPath = "uploads/avatar/" + fileName;
            userService.updateAvatar(userId, avatarPath);

            return Result.success(avatarPath);
        } catch (IOException e) {
            return Result.error(400, "无法读取或保存头像，请检查图片后重试");
        }
    }
}
