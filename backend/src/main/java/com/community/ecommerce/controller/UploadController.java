package com.community.ecommerce.controller;

import com.community.ecommerce.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 图片上传接口（演示环境直接回显 URL，生产替换为对象存储）
 * POST /api/upload {url} -> {url}
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @PostMapping
    public ApiResponse upload(@RequestBody Map<String, Object> body) {
        Object url = body.get("url");
        String result = url != null && !String.valueOf(url).isBlank()
                ? String.valueOf(url)
                : "https://picsum.photos/seed/" + UUID.randomUUID().toString().substring(0, 8) + "/600/600";
        return ApiResponse.ok(Map.of("url", result));
    }
}
