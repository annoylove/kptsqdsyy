package com.community.ecommerce.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/** 订单状态机单元测试：7 条合法流转 + 非法流转拦截 */
class OrderStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING_PAYMENT, PAY, PENDING_SHIPMENT",
            "PENDING_PAYMENT, CANCEL, CANCELLED",
            "PENDING_SHIPMENT, SHIP, PENDING_RECEIPT",
            "PENDING_RECEIPT, CONFIRM_RECEIPT, COMPLETED",
            "COMPLETED, APPLY_REFUND, REFUNDING",
            "REFUNDING, AGREE_REFUND, REFUNDED"
    })
    void legalTransitions(String from, String event, String to) {
        assertTrue(OrderStateMachine.canTransition(from, event));
        assertEquals(to, OrderStateMachine.transition(from, event));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING_PAYMENT, SHIP",        // 未支付不能发货
            "PENDING_PAYMENT, CONFIRM_RECEIPT",
            "PENDING_SHIPMENT, PAY",        // 重复支付
            "PENDING_SHIPMENT, CANCEL",     // 发货前仅待付款可取消（支付后不可取消）
            "PENDING_RECEIPT, SHIP",        // 重复发货
            "COMPLETED, SHIP",              // 完成后不能发货
            "COMPLETED, CANCEL",            // 完成后不能取消
            "CANCELLED, PAY",               // 终态无事件
            "CANCELLED, CANCEL",
            "REFUNDED, PAY",                // 终态无事件
            "REFUNDED, APPLY_REFUND"
    })
    void illegalTransitionsRejected(String status, String event) {
        assertFalse(OrderStateMachine.canTransition(status, event));
        BizException ex = assertThrows(BizException.class,
                () -> OrderStateMachine.transition(status, event));
        assertEquals(400, ex.statusCode);
    }

    @Test
    void unknownStatusRejected() {
        assertFalse(OrderStateMachine.canTransition("UNKNOWN", "PAY"));
        assertThrows(BizException.class, () -> OrderStateMachine.transition("UNKNOWN", "PAY"));
    }
}
