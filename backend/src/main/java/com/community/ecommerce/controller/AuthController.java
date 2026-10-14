package com.community.ecommerce.controller;

import com.community.ecommerce.common.ApiResponse;
import com.community.ecommerce.common.BizException;
import com.community.ecommerce.service.TokenService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口（演示用户内置，生产对接短信/微信授权）
 * POST /api/auth/login   {phone, code} -> {accessToken, refreshToken, user}
 * POST /api/auth/refresh {refreshToken} -> {accessToken, refreshToken}
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 用户表：手机号 -> {code, userId, nickname, avatar, phone}（演示内置账号 + 注册用户） */
    private static final Map<String, Map<String, String>> DEMO_USERS = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        DEMO_USERS.put("13800008888", Map.of(
                "code", "8888",
                "userId", "U001",
                "nickname", "山间来客",
                "avatar", "https://picsum.photos/seed/u001/200/200",
                "phone", "138****8888"));
    }

    /** 固定演示验证码 */
    private static final String DEMO_CODE = "8888";

    private final TokenService tokenService;

    public AuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody Map<String, String> body) {
        Map<String, String> user = DEMO_USERS.get(body.get("phone"));
        if (user == null || !DEMO_CODE.equals(body.get("code"))) {
            throw new BizException(400, "手机号或验证码错误");
        }
        String userId = user.get("userId");
        Map<String, Object> data = ApiResponse.map(
                "accessToken", tokenService.issueAccessToken(userId),
                "refreshToken", tokenService.issueRefreshToken(userId),
                "user", ApiResponse.map(
                        "id", userId,
                        "nickname", user.get("nickname"),
                        "avatar", user.get("avatar"),
                        "phone", user.get("phone")));
        return ApiResponse.ok(data);
    }

    /**
     * 注册：POST /api/auth/register {phone, code, nickname}
     * 演示环境固定验证码 8888；注册成功直接返回登录态双 Token。
     */
    @PostMapping("/register")
    public ApiResponse register(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        String nickname = body.get("nickname");
        if (phone == null || !phone.matches("1\\d{10}")) throw new BizException(400, "手机号格式不正确");
        if (!DEMO_CODE.equals(code)) throw new BizException(400, "验证码错误");
        if (DEMO_USERS.containsKey(phone)) throw new BizException(400, "该手机号已注册");
        if (nickname == null || nickname.isBlank()) nickname = "用户" + phone.substring(7);

        String userId = "U" + String.format("%03d", DEMO_USERS.size() + 1);
        String masked = phone.substring(0, 3) + "****" + phone.substring(7);
        Map<String, String> u = new HashMap<>();
        u.put("code", DEMO_CODE);
        u.put("userId", userId);
        u.put("nickname", nickname);
        u.put("avatar", "https://picsum.photos/seed/" + userId + "/200/200");
        u.put("phone", masked);
        DEMO_USERS.put(phone, u);

        Map<String, Object> data = ApiResponse.map(
                "accessToken", tokenService.issueAccessToken(userId),
                "refreshToken", tokenService.issueRefreshToken(userId),
                "user", ApiResponse.map(
                        "id", userId,
                        "nickname", nickname,
                        "avatar", u.get("avatar"),
                        "phone", masked));
        return ApiResponse.ok(data);
    }

    @PostMapping("/refresh")
    public ApiResponse refresh(@RequestBody Map<String, String> body) {
        String userId = tokenService.refresh(body.get("refreshToken"));
        Map<String, Object> data = ApiResponse.map(
                "accessToken", tokenService.issueAccessToken(userId),
                "refreshToken", tokenService.issueRefreshToken(userId));
        return ApiResponse.ok(data);
    }
}
