package com.zhikao.controller;

import com.zhikao.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.badRequest("文件为空");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|png|bmp)")) {
            return Result.badRequest("仅支持 JPG/PNG/BMP 格式");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.badRequest("图片不能超过 2MB");
        }

        try {
            Path dir = Paths.get(uploadDir, "avatars");
            Files.createDirectories(dir);

            String ext = contentType.substring(contentType.indexOf('/') + 1);
            if ("jpeg".equals(ext)) ext = "jpg";
            String filename = UUID.randomUUID().toString() + "." + ext;
            Path filepath = dir.resolve(filename);
            file.transferTo(filepath.toFile());

            Map<String, String> data = new HashMap<>();
            data.put("url", "/api/v1/uploads/avatars/" + filename);
            return Result.success("上传成功", data);
        } catch (IOException e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
