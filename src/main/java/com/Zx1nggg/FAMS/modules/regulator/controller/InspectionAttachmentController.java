package com.Zx1nggg.FAMS.modules.regulator.controller;

import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/** 附件下载通过监管角色鉴权，不挂载到公开 uploads 静态目录。 */
@RestController
@RequestMapping("/regulator/inspections/attachments")
public class InspectionAttachmentController {
    @Value("${app.upload.inspection-dir:uploads/inspection}") private String directory;
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw new BusinessException(400, "附件大小须为 1 字节至 5 MB");
        byte[] bytes = file.getBytes();
        String extension;
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') extension = ".png";
        else if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff) extension = ".jpg";
        else if (bytes.length >= 5 && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) extension = ".pdf";
        else throw new BusinessException(400, "仅支持 PNG、JPEG 或 PDF 附件");
        Path root = Path.of(directory).toAbsolutePath().normalize(); Files.createDirectories(root);
        String name = UUID.randomUUID() + extension;
        Files.write(root.resolve(name), bytes, StandardOpenOption.CREATE_NEW);
        return Result.success("/api/regulator/inspections/attachments/" + name);
    }
    @GetMapping("/{name}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String name) {
        if (!name.matches("[a-f0-9-]+\\.(png|jpg|pdf)")) throw new BusinessException(400, "附件名称不合法");
        Path root = Path.of(directory).toAbsolutePath().normalize(); Path file = root.resolve(name).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) throw new BusinessException(404, "附件不存在");
        return ResponseEntity.ok().header("Content-Type", "application/octet-stream")
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                .header("X-Content-Type-Options", "nosniff").body(new FileSystemResource(file));
    }
}
