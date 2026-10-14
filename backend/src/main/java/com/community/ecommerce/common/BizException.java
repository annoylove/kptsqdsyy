package com.community.ecommerce.common;

/** 业务异常：statusCode 用于 HTTP 状态（401 触发前端无感刷新），code 用于业务码 */
public class BizException extends RuntimeException {
    public final int statusCode;
    public final int code;

    public BizException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = statusCode;
    }

    public BizException(int statusCode, int code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }
}
