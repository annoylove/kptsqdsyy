package com.community.ecommerce.service;

import com.community.ecommerce.common.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/** TokenService 单元测试：签发、校验、过期、一次性轮换 */
class TokenServiceTest {

    private TokenService service;

    @BeforeEach
    void setUp() {
        service = new TokenService();
        ReflectionTestUtils.setField(service, "accessTtl", 300L);
        ReflectionTestUtils.setField(service, "refreshTtl", 604800L);
    }

    @Test
    void validateAccessTokenReturnsUserId() {
        String at = service.issueAccessToken("U001");
        assertEquals("U001", service.validateAccessToken("Bearer " + at));
        // 无前缀也应可用
        assertEquals("U001", service.validateAccessToken(at));
    }

    @Test
    void blankOrFakeTokenRejected() {
        assertBiz401(() -> service.validateAccessToken(null));
        assertBiz401(() -> service.validateAccessToken(""));
        assertBiz401(() -> service.validateAccessToken("Bearer FAKE"));
    }

    @Test
    void expiredAccessTokenRejected() {
        ReflectionTestUtils.setField(service, "accessTtl", -1L); // 立即过期
        String at = service.issueAccessToken("U001");
        assertBiz401(() -> service.validateAccessToken(at));
    }

    @Test
    void refreshTokenOneTimeRotation() {
        String rt = service.issueRefreshToken("U001");
        assertEquals("U001", service.refresh(rt));
        // 第二次使用同一 refresh token 必须失败（一次性轮换）
        assertBiz401(() -> service.refresh(rt));
    }

    @Test
    void blankRefreshTokenRejected() {
        assertBiz401(() -> service.refresh(null));
        assertBiz401(() -> service.refresh(""));
    }

    private void assertBiz401(Runnable r) {
        BizException e = assertThrows(BizException.class, r::run);
        assertEquals(401, e.statusCode);
    }
}
