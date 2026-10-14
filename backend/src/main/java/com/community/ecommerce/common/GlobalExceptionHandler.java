package com.community.ecommerce.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** 全局异常处理：BizException -> 对应 HTTP 状态码 + {code,message}，前端拦截器按 statusCode 触发 401 无感刷新 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException e) {
        return ResponseEntity.status(e.statusCode)
                .body(Map.of("code", e.code, "message", e.getMessage() == null ? "请求失败" : e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        return ResponseEntity.status(500)
                .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
    }
}
