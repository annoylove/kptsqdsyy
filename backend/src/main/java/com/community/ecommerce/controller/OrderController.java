package com.community.ecommerce.controller;

import com.community.ecommerce.common.ApiResponse;
import com.community.ecommerce.common.BizException;
import com.community.ecommerce.common.CurrentUser;
import com.community.ecommerce.common.OrderStateMachine;
import com.community.ecommerce.entity.Goods;
import com.community.ecommerce.entity.Order;
import com.community.ecommerce.entity.OrderItem;
import com.community.ecommerce.entity.Sku;
import com.community.ecommerce.repository.GoodsRepository;
import com.community.ecommerce.repository.OrderRepository;
import com.community.ecommerce.repository.SkuRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 订单接口（状态机驱动，所有流转经 OrderStateMachine 校验）
 * POST /api/order/create  {items}              -> 下单（待付款，扣减库存）
 * GET  /api/order/list?status=                 -> 我的订单
 * GET  /api/order/detail?orderNo=              -> 订单详情
 * POST /api/order/pay     {orderNo}            -> 支付
 * POST /api/order/cancel  {orderNo}            -> 取消（回补库存）
 * POST /api/order/ship    {orderNo,trackingNo} -> 发货
 * POST /api/order/confirm {orderNo}            -> 确认收货
 * POST /api/order/refund/apply {orderNo}       -> 申请售后
 * POST /api/order/refund/agree {orderNo}       -> 同意退款
 * POST /api/stock/check   {items:[{skuId,quantity}]} -> 结算前库存二次拦截
 */
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderRepository orderRepo;
    private final GoodsRepository goodsRepo;
    private final SkuRepository skuRepo;
    /** 同毫秒并发下单时保证订单号唯一：纳秒时间戳 + 进程内自增序号 */
    private static final java.util.concurrent.atomic.AtomicLong ORDER_SEQ = new java.util.concurrent.atomic.AtomicLong();

    public OrderController(OrderRepository orderRepo, GoodsRepository goodsRepo, SkuRepository skuRepo) {
        this.orderRepo = orderRepo;
        this.goodsRepo = goodsRepo;
        this.skuRepo = skuRepo;
    }

    @PostMapping("/order/create")
    @Transactional
    public ApiResponse create(@RequestBody Map<String, Object> body) {
        String userId = CurrentUser.id();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) throw new BizException(400, "订单商品不能为空");

        Order order = new Order();
        order.setId(UUID.randomUUID().toString().replace("-", ""));
        order.setOrderNo("NO" + System.currentTimeMillis() + String.format("%04d", ORDER_SEQ.incrementAndGet() % 10000));
        order.setUserId(userId);
        order.setStatus(OrderStateMachine.PENDING_PAYMENT);
        order.setCreateTime(System.currentTimeMillis());

        double total = 0;
        for (Map<String, Object> it : items) {
            String skuId = String.valueOf(it.get("skuId"));
            int quantity = ((Number) it.get("quantity")).intValue();
            Sku sku = skuRepo.findById(skuId).orElseThrow(() -> new BizException(400, "规格已失效: " + skuId));
            Goods goods = goodsRepo.findById(sku.getGoodsId())
                    .orElseThrow(() -> new BizException(400, "商品不存在"));
            if (!"on".equals(goods.getStatus())) throw new BizException(400, "商品已下架: " + goods.getTitle());
            // 原子条件扣减（UPDATE ... WHERE stock >= qty），并发安全防超卖；返回 0 即库存不足
            if (skuRepo.deductStock(skuId, quantity) == 0) {
                int remain = skuRepo.findById(skuId).map(Sku::getStock).orElse(0);
                throw new BizException(400, "库存不足，仅剩" + remain + "件");
            }

            OrderItem oi = new OrderItem();
            oi.setGoodsId(goods.getId());
            oi.setSkuId(skuId);
            oi.setShopId(goods.getShopId());
            oi.setShopName(shopName(goods.getShopId()));
            oi.setTitle(goods.getTitle());
            oi.setCover(goods.getCover());
            oi.setSpecText(String.valueOf(it.getOrDefault("specText", "")));
            oi.setPrice(sku.getPrice());
            oi.setQuantity(quantity);
            order.getItems().add(oi);
            total += sku.getPrice() * quantity;
        }
        order.setTotalAmount(round(total));
        orderRepo.save(order);
        return ApiResponse.ok(toDto(order));
    }

    @GetMapping("/order/list")
    public ApiResponse list(@RequestParam(required = false) String status) {
        String userId = CurrentUser.id();
        List<Order> orders = (status == null || status.isBlank())
                ? orderRepo.findByUserIdOrderByCreateTimeDesc(userId)
                : orderRepo.findByUserIdAndStatusOrderByCreateTimeDesc(userId, status);
        return ApiResponse.ok(ApiResponse.map("list", orders.stream().map(this::toDto).toList()));
    }

    @GetMapping("/order/detail")
    public ApiResponse detail(@RequestParam String orderNo) {
        return ApiResponse.ok(toDto(find(orderNo)));
    }

    @PostMapping("/order/pay")
    public ApiResponse pay(@RequestBody Map<String, Object> body) {
        Order order = find(String.valueOf(body.get("orderNo")));
        order.setStatus(OrderStateMachine.transition(order.getStatus(), OrderStateMachine.PAY));
        order.setPayTime(System.currentTimeMillis());
        orderRepo.save(order);
        return ApiResponse.ok(toDto(order));
    }

    @PostMapping("/order/cancel")
    @Transactional
    public ApiResponse cancel(@RequestBody Map<String, Object> body) {
        Order order = find(String.valueOf(body.get("orderNo")));
        order.setStatus(OrderStateMachine.transition(order.getStatus(), OrderStateMachine.CANCEL));
        order.setCancelReason(String.valueOf(body.getOrDefault("reason", "用户取消")));
        restoreStock(order);
        orderRepo.save(order);
        return ApiResponse.ok(toDto(order));
    }

    @PostMapping("/order/ship")
    public ApiResponse ship(@RequestBody Map<String, Object> body) {
        Order order = find(String.valueOf(body.get("orderNo")));
        order.setStatus(OrderStateMachine.transition(order.getStatus(), OrderStateMachine.SHIP));
        order.setTrackingNo(String.valueOf(body.getOrDefault("trackingNo", "SF" + System.currentTimeMillis())));
        order.setShipTime(System.currentTimeMillis());
        orderRepo.save(order);
        return ApiResponse.ok(toDto(order));
    }

    @PostMapping("/order/confirm")
    public ApiResponse confirm(@RequestBody Map<String, Object> body) {
        Order order = find(String.valueOf(body.get("orderNo")));
        order.setStatus(OrderStateMachine.transition(order.getStatus(), OrderStateMachine.CONFIRM_RECEIPT));
        order.setFinishTime(System.currentTimeMillis());
        orderRepo.save(order);
        return ApiResponse.ok(toDto(order));
    }

    @PostMapping("/order/refund/apply")
    public ApiResponse applyRefund(@RequestBody Map<String, Object> body) {
        Order order = find(String.valueOf(body.get("orderNo")));
        order.setStatus(OrderStateMachine.transition(order.getStatus(), OrderStateMachine.APPLY_REFUND));
        orderRepo.save(order);
        return ApiResponse.ok(toDto(order));
    }

    @PostMapping("/order/refund/agree")
    @Transactional
    public ApiResponse agreeRefund(@RequestBody Map<String, Object> body) {
        Order order = find(String.valueOf(body.get("orderNo")));
        order.setStatus(OrderStateMachine.transition(order.getStatus(), OrderStateMachine.AGREE_REFUND));
        restoreStock(order); // 退款回补库存
        orderRepo.save(order);
        return ApiResponse.ok(toDto(order));
    }

    /** 结算前库存二次拦截：后端库存临时变动时前端能收到失效原因 */
    @PostMapping("/stock/check")
    public ApiResponse checkStock(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        List<Map<String, Object>> invalid = new ArrayList<>();
        for (Map<String, Object> it : items) {
            String skuId = String.valueOf(it.get("skuId"));
            int quantity = ((Number) it.get("quantity")).intValue();
            Optional<Sku> skuOpt = skuRepo.findById(skuId);
            if (skuOpt.isEmpty()) {
                invalid.add(ApiResponse.map("skuId", skuId, "reason", "规格已失效"));
                continue;
            }
            Sku sku = skuOpt.get();
            Goods goods = goodsRepo.findById(sku.getGoodsId()).orElse(null);
            if (goods == null || !"on".equals(goods.getStatus())) {
                invalid.add(ApiResponse.map("skuId", skuId, "reason", "商品已下架"));
            } else if (sku.getStock() < quantity) {
                invalid.add(ApiResponse.map("skuId", skuId, "reason", "库存不足，仅剩" + sku.getStock() + "件"));
            }
        }
        return ApiResponse.ok(ApiResponse.map("valid", invalid.isEmpty(), "invalid", invalid));
    }

    private Order find(String orderNo) {
        return orderRepo.findByOrderNo(orderNo).orElseThrow(() -> new BizException(404, "订单不存在"));
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            skuRepo.restoreStock(item.getSkuId(), item.getQuantity());
        }
    }

    private String shopName(String shopId) {
        return switch (shopId == null ? "" : shopId) {
            case "S001" -> "优选数码旗舰店";
            case "S002" -> "轻食生活馆";
            case "S003" -> "山野户外专营店";
            default -> "";
        };
    }

    private double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private Map<String, Object> toDto(Order o) {
        List<Map<String, Object>> items = o.getItems().stream().map(i -> ApiResponse.map(
                "goodsId", i.getGoodsId(),
                "skuId", i.getSkuId(),
                "shopId", i.getShopId(),
                "shopName", i.getShopName(),
                "title", i.getTitle(),
                "cover", i.getCover(),
                "specText", i.getSpecText(),
                "price", i.getPrice(),
                "quantity", i.getQuantity())).toList();
        Map<String, Object> m = new HashMap<>();
        m.put("id", o.getId());
        m.put("orderNo", o.getOrderNo());
        m.put("status", o.getStatus());
        m.put("totalAmount", o.getTotalAmount());
        m.put("createTime", o.getCreateTime());
        m.put("payTime", o.getPayTime());
        m.put("shipTime", o.getShipTime());
        m.put("finishTime", o.getFinishTime());
        m.put("trackingNo", o.getTrackingNo());
        m.put("cancelReason", o.getCancelReason());
        m.put("items", items);
        return m;
    }
}
