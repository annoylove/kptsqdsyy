package com.community.ecommerce.common;

import java.util.*;

/**
 * 订单状态机（与前端 utils/orderStateMachine.js 保持同一份流转规则）
 * 后端是最终一致性防线：非法流转直接拒绝。
 */
public final class OrderStateMachine {

    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String PENDING_SHIPMENT = "PENDING_SHIPMENT";
    public static final String PENDING_RECEIPT = "PENDING_RECEIPT";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUNDING = "REFUNDING";
    public static final String REFUNDED = "REFUNDED";

    public static final String PAY = "PAY";
    public static final String CANCEL = "CANCEL";
    public static final String SHIP = "SHIP";
    public static final String CONFIRM_RECEIPT = "CONFIRM_RECEIPT";
    public static final String APPLY_REFUND = "APPLY_REFUND";
    public static final String AGREE_REFUND = "AGREE_REFUND";

    private static final Map<String, Map<String, String>> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put(PENDING_PAYMENT, Map.of(PAY, PENDING_SHIPMENT, CANCEL, CANCELLED));
        TRANSITIONS.put(PENDING_SHIPMENT, Map.of(SHIP, PENDING_RECEIPT));
        TRANSITIONS.put(PENDING_RECEIPT, Map.of(CONFIRM_RECEIPT, COMPLETED));
        TRANSITIONS.put(COMPLETED, Map.of(APPLY_REFUND, REFUNDING));
        TRANSITIONS.put(REFUNDING, Map.of(AGREE_REFUND, REFUNDED));
        TRANSITIONS.put(CANCELLED, Map.of());
        TRANSITIONS.put(REFUNDED, Map.of());
    }

    private OrderStateMachine() {}

    public static boolean canTransition(String status, String event) {
        Map<String, String> m = TRANSITIONS.get(status);
        return m != null && m.containsKey(event);
    }

    /** 非法流转抛 BizException -> 全局异常处理返回 400 */
    public static String transition(String status, String event) {
        if (!canTransition(status, event)) {
            throw new BizException(400, "非法订单流转: 状态[" + status + "] 不支持事件[" + event + "]");
        }
        return TRANSITIONS.get(status).get(event);
    }
}
