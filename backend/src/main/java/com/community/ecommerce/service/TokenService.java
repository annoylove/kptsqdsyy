package com.community.ecommerce.service;

import com.community.ecommerce.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 服务：Access Token（短期）+ Refresh Token（一次性轮换）
 * 演示环境用内存存储；生产应使用 Redis + JWT。
 */
@Service
public class TokenService {

    private static class TokenInfo {
        final String userId;
        final long expireAt;
        TokenInfo(String userId, long ttlSeconds) {
            this.userId = userId;
            this.expireAt = System.currentTimeMillis() + ttlSeconds * 1000;
        }
        boolean valid() { return System.currentTimeMillis() < expireAt; }
    }

    private final Map<String, TokenInfo> accessTokens = new ConcurrentHashMap<>();
    private final Map<String, TokenInfo> refreshTokens = new ConcurrentHashMap<>();

    @Value("${app.access-token-ttl}")
    private long accessTtl;

    @Value("${app.refresh-token-ttl}")
    private long refreshTtl;

    public String issueAccessToken(String userId) {
        String token = "AT_" + UUID.randomUUID().toString().replace("-", "");
        accessTokens.put(token, new TokenInfo(userId, accessTtl));
        return token;
    }

    public String issueRefreshToken(String userId) {
        String token = "RT_" + UUID.randomUUID().toString().replace("-", "");
        refreshTokens.put(token, new TokenInfo(userId, refreshTtl));
        return token;
    }

    public String validateAccessToken(String bearer) {
        if (bearer == null || bearer.isBlank()) throw new BizException(401, "未登录");
        String token = bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
        TokenInfo info = accessTokens.get(token);
        if (info == null || !info.valid()) {
            accessTokens.remove(token);
            throw new BizException(401, "Token 已过期");
        }
        return info.userId;
    }

    /** 刷新：旧 refreshToken 立即失效（轮换），返回 userId */
    public String refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) throw new BizException(401, "Refresh Token 无效");
        TokenInfo info = refreshTokens.remove(refreshToken);
        if (info == null || !info.valid()) throw new BizException(401, "Refresh Token 无效");
        return info.userId;
    }

    /** 定时清理过期 token，防内存泄漏 */
    public void purgeExpired() {
        accessTokens.entrySet().removeIf(e -> !e.getValue().valid());
        refreshTokens.entrySet().removeIf(e -> !e.getValue().valid());
    }
}
