package com.community.ecommerce.config;

import com.community.ecommerce.common.BizException;
import com.community.ecommerce.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 鉴权拦截器 + CORS（H5 前端 localhost:5173 跨域） */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TokenService tokenService;

    public WebConfig(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                        // OPTIONS 预检直接放行
                        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
                        String userId = tokenService.validateAccessToken(req.getHeader("Authorization"));
                        req.setAttribute("userId", userId);
                        return true;
                    }
                })
                .addPathPatterns(
                        "/api/order/**",        // 订单全部需登录
                        "/api/post/publish",    // 发布需登录
                        "/api/upload"
                )
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/goods/**",
                        "/api/shop/**",
                        "/api/post/list",
                        "/api/post/detail",
                        "/api/post/like",
                        "/api/stock/check"
                );
    }
}
