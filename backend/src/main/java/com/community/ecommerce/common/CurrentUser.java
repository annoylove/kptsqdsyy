package com.community.ecommerce.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 从当前请求解析登录用户 ID（拦截器已校验 Authorization） */
public final class CurrentUser {
    private CurrentUser() {}

    public static String id() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new BizException(401, "未登录");
        Object uid = attrs.getRequest().getAttribute("userId");
        if (uid == null) throw new BizException(401, "未登录");
        return (String) uid;
    }
}
