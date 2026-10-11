package com.campus.volunteer.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.campus.volunteer.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 图片上传控制器（需登录）
 * 上传目录由配置项 upload.dir 指定（默认 ./uploads），不再硬编码 nginx 目录；
 * 静态访问走 /uploads/**（MvcConfig 已做资源映射）
 */
@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    /** 允许的图片后缀 */
    private static final List<String> ALLOWED_SUFFIX = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");

    /** 最大5MB */
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    /**
     * 上传图片，返回可访问的相对URL /uploads/{fileName}
     */
    @PostMapping("/image")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            // 1.校验原始文件名
            String originalFilename = image.getOriginalFilename();
            if (StrUtil.isBlank(originalFilename) || !originalFilename.contains(".")) {
                return Result.fail("文件名不合法");
            }
            // 2.校验后缀与大小
            String suffix = StrUtil.subAfter(originalFilename, ".", true).toLowerCase();
            if (!ALLOWED_SUFFIX.contains(suffix)) {
                return Result.fail("仅支持图片文件（png/jpg/jpeg/gif/webp）");
            }
            if (image.getSize() > MAX_SIZE) {
                return Result.fail("图片大小不能超过5MB");
            }
            // 3.生成新文件名并保存
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
            File dir = new File(uploadDir);
            if (!dir.exists() && !dir.mkdirs()) {
                return Result.fail("上传目录创建失败");
            }
            // 注意：MultipartFile.transferTo 传相对路径时会解析到容器临时目录而非工作目录，
            // 必须转成绝对路径再写入（经典坑）
            File dest = new File(dir, fileName).getAbsoluteFile();
            image.transferTo(dest);
            log.info("图片上传成功：{}", dest.getAbsolutePath());
            // 4.返回可访问URL
            return Result.ok("/uploads/" + fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除图片（仅允许文件名，防目录穿越）
     */
    @GetMapping("/image/delete")
    public Result deleteImage(@RequestParam("name") String filename) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return Result.fail("错误的文件名称");
        }
        File file = new File(uploadDir, filename);
        if (file.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(file);
        return Result.ok();
    }
}
