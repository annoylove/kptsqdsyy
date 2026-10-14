package com.community.ecommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 全链路集成测试（H2 隔离库）：
 * 登录 -> 下单扣库存 -> 支付 -> 发货 -> 确认 -> 售后 -> 退款回补，
 * 以及鉴权 401、非法流转 400、库存二次拦截。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EcommerceApiIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired com.community.ecommerce.repository.SkuRepository skuRepo;

    private String token = null;

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private int status(MvcResult r) {
        return r.getResponse().getStatus();
    }

    private void login() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800008888\",\"code\":\"8888\"}"))
                .andReturn();
        assertEquals(200, status(r));
        token = body(r).path("data").path("accessToken").asText();
        assertFalse(token.isBlank());
    }

    private int skuStock(String skuId) throws Exception {
        MvcResult r = mvc.perform(get("/api/goods/detail").param("id", skuId.substring(0, 4))).andReturn();
        for (JsonNode s : body(r).path("data").path("skus")) {
            if (skuId.equals(s.path("id").asText())) return s.path("stock").asInt();
        }
        fail("sku not found: " + skuId);
        return -1;
    }

    @Test
    @Order(1)
    void publicEndpointsWork() throws Exception {
        assertEquals(200, status(mvc.perform(get("/api/goods/list")).andReturn()));
        assertEquals(200, status(mvc.perform(get("/api/shop/list")).andReturn()));
        assertEquals(200, status(mvc.perform(get("/api/post/list")).andReturn()));
    }

    @Test
    @Order(2)
    void wrongCodeRejected() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800008888\",\"code\":\"0000\"}"))
                .andReturn();
        assertEquals(400, status(r));
    }

    @Test
    @Order(3)
    void protectedEndpointWithoutToken401() throws Exception {
        assertEquals(401, status(mvc.perform(get("/api/order/list")).andReturn()));
        assertEquals(401, status(mvc.perform(get("/api/order/list")
                .header("Authorization", "Bearer FAKE")).andReturn()));
    }

    @Test
    @Order(4)
    void fullOrderLifecycleWithStockAndStateMachine() throws Exception {
        login();
        String auth = "Bearer " + token;

        // 下单：扣库存
        int before = skuStock("G001-A");
        MvcResult created = mvc.perform(post("/api/order/create")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"skuId\":\"G001-A\",\"quantity\":2,\"specText\":\"曜石黑/标准版\"}]}"))
                .andReturn();
        assertEquals(200, status(created));
        String orderNo = body(created).path("data").path("orderNo").asText();
        assertEquals("PENDING_PAYMENT", body(created).path("data").path("status").asText());
        assertEquals(before - 2, skuStock("G001-A"));

        // 支付
        MvcResult paid = mvc.perform(post("/api/order/pay")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"" + orderNo + "\"}"))
                .andReturn();
        assertEquals(200, status(paid));
        assertEquals("PENDING_SHIPMENT", body(paid).path("data").path("status").asText());

        // 重复支付 -> 非法流转 400
        assertEquals(400, status(mvc.perform(post("/api/order/pay")
                .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"" + orderNo + "\"}")).andReturn()));

        // 发货
        MvcResult shipped = mvc.perform(post("/api/order/ship")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"" + orderNo + "\",\"trackingNo\":\"SF123\"}"))
                .andReturn();
        assertEquals("PENDING_RECEIPT", body(shipped).path("data").path("status").asText());

        // 确认收货
        MvcResult done = mvc.perform(post("/api/order/confirm")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"" + orderNo + "\"}"))
                .andReturn();
        assertEquals("COMPLETED", body(done).path("data").path("status").asText());

        // 售后申请 -> 同意 -> 库存回补
        MvcResult refunding = mvc.perform(post("/api/order/refund/apply")
                .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"" + orderNo + "\"}")).andReturn();
        assertEquals("REFUNDING", body(refunding).path("data").path("status").asText());

        int mid = skuStock("G001-A");
        MvcResult refunded = mvc.perform(post("/api/order/refund/agree")
                .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"" + orderNo + "\"}")).andReturn();
        assertEquals("REFUNDED", body(refunded).path("data").path("status").asText());
        assertEquals(mid + 2, skuStock("G001-A"));

        // 终态后任何事件都非法
        assertEquals(400, status(mvc.perform(post("/api/order/refund/apply")
                .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"" + orderNo + "\"}")).andReturn()));

        // 订单列表/详情可查
        MvcResult list = mvc.perform(get("/api/order/list").header("Authorization", auth)).andReturn();
        assertEquals(200, status(list));
        boolean found = false;
        for (JsonNode o : body(list).path("data").path("list")) {
            if (orderNo.equals(o.path("orderNo").asText())) found = true;
        }
        assertTrue(found);
        assertEquals(200, status(mvc.perform(get("/api/order/detail")
                .param("orderNo", orderNo).header("Authorization", auth)).andReturn()));
    }

    @Test
    @Order(5)
    void cancelRestoresStock() throws Exception {
        if (token == null) login();
        String auth = "Bearer " + token;
        int before = skuStock("G003-A");
        MvcResult created = mvc.perform(post("/api/order/create")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"skuId\":\"G003-A\",\"quantity\":3}]}"))
                .andReturn();
        String orderNo = body(created).path("data").path("orderNo").asText();
        assertEquals(before - 3, skuStock("G003-A"));

        MvcResult cancelled = mvc.perform(post("/api/order/cancel")
                .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"" + orderNo + "\"}")).andReturn();
        assertEquals("CANCELLED", body(cancelled).path("data").path("status").asText());
        assertEquals(before, skuStock("G003-A"));
    }

    @Test
    @Order(6)
    void stockCheckBlocksOverStockAndOffSale() throws Exception {
        MvcResult r = mvc.perform(post("/api/stock/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"skuId\":\"G001-A\",\"quantity\":99999},{\"skuId\":\"G004-A\",\"quantity\":1}]}"))
                .andReturn();
        assertEquals(200, status(r));
        JsonNode data = body(r).path("data");
        assertFalse(data.path("valid").asBoolean());
        assertEquals(2, data.path("invalid").size()); // 超库存 + 下架商品
    }

    @Test
    @Order(7)
    void oversellRejectedOnCreate() throws Exception {
        if (token == null) login();
        MvcResult r = mvc.perform(post("/api/order/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"skuId\":\"G001-D\",\"quantity\":1}]}")) // 库存 0
                .andReturn();
        assertEquals(400, status(r));
        assertTrue(body(r).path("message").asText().contains("库存不足"));
    }

    @Test
    @Order(8)
    void tokenRefreshRotation() throws Exception {
        MvcResult loginRes = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800008888\",\"code\":\"8888\"}"))
                .andReturn();
        String rt = body(loginRes).path("data").path("refreshToken").asText();

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rt + "\"}"))
                .andReturn();
        assertEquals(200, status(refreshed));
        assertFalse(body(refreshed).path("data").path("accessToken").asText().isBlank());

        // 旧 refresh token 复用 -> 401
        assertEquals(401, status(mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rt + "\"}")).andReturn()));
    }

    /**
     * 并发防超卖：库存 5 件，10 线程同时各抢 1 件。
     * 不变式：成功下单数 + 剩余库存 == 初始库存，且剩余库存 >= 0（原子条件扣减保证）。
     */
    @Test
    @Order(9)
    void concurrentOrdersNeverOversell() throws Exception {
        if (token == null) login();
        String auth = "Bearer " + token;

        int initialStock = 5;
        com.community.ecommerce.entity.Sku sku =
                skuRepo.findById("G001-B").orElseThrow();
        sku.setStock(initialStock);
        skuRepo.save(sku);

        int threads = 10;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger success = new java.util.concurrent.atomic.AtomicInteger();
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    MvcResult r = mvc.perform(post("/api/order/create")
                                    .header("Authorization", auth)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"items\":[{\"skuId\":\"G001-B\",\"quantity\":1}]}"))
                            .andReturn();
                    if (status(r) == 200) success.incrementAndGet();
                } catch (Exception ignored) {
                    // 请求异常视为失败
                }
            }));
        }
        start.countDown();
        for (java.util.concurrent.Future<?> f : futures) f.get(30, java.util.concurrent.TimeUnit.SECONDS);
        pool.shutdown();

        int finalStock = skuRepo.findById("G001-B").orElseThrow().getStock();
        assertTrue(finalStock >= 0, "库存不能为负（超卖）");
        assertTrue(success.get() <= initialStock, "成功订单数不能超过库存");
        assertEquals(initialStock, success.get() + finalStock, "扣减必须与成功订单严格一致");
    }
}
