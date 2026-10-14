package com.community.ecommerce.common;

import java.util.HashMap;
import java.util.Map;

/** 统一响应体：{ code, message, data }，与前端 mock 返回结构一致 */
public class ApiResponse {
    public int code;
    public String message;
    public Object data;

    public ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse ok(Object data) {
        return new ApiResponse(0, "ok", data);
    }

    public static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
