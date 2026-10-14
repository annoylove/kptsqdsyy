/**
 * 订单状态机（Order State Machine）
 * 严格约束合法流转，杜绝非法状态跳转（电商最易出 Bug 处）。
 *
 * 状态流转图：
 *   [待付款] --支付--> [待发货] --发货--> [待收货] --确认收货--> [已完成]
 *      |                                    (发货后15天自动确认)     |
 *      |--超时30min/用户取消--> [已取消]                              |--申请售后--> [售后中] --商家同意--> [已退款]
 */

export const OrderStatus = {
  PENDING_PAYMENT: 'PENDING_PAYMENT', // 待付款
  PENDING_SHIPMENT: 'PENDING_SHIPMENT', // 待发货
  PENDING_RECEIPT: 'PENDING_RECEIPT', // 待收货
  COMPLETED: 'COMPLETED', // 已完成
  CANCELLED: 'CANCELLED', // 已取消
  REFUNDING: 'REFUNDING', // 售后中
  REFUNDED: 'REFUNDED' // 已退款
}

// 触发事件
export const OrderEvent = {
  PAY: 'PAY', // 支付
  CANCEL: 'CANCEL', // 取消（用户主动 / 超时）
  SHIP: 'SHIP', // 发货
  CONFIRM_RECEIPT: 'CONFIRM_RECEIPT', // 确认收货
  APPLY_REFUND: 'APPLY_REFUND', // 申请售后
  AGREE_REFUND: 'AGREE_REFUND' // 商家同意退款
}

// 状态 -> 中文文案
export const OrderStatusText = {
  [OrderStatus.PENDING_PAYMENT]: '待付款',
  [OrderStatus.PENDING_SHIPMENT]: '待发货',
  [OrderStatus.PENDING_RECEIPT]: '待收货',
  [OrderStatus.COMPLETED]: '已完成',
  [OrderStatus.CANCELLED]: '已取消',
  [OrderStatus.REFUNDING]: '售后中',
  [OrderStatus.REFUNDED]: '已退款'
}

// 状态 -> 该状态下可执行的操作事件
const TRANSITIONS = {
  [OrderStatus.PENDING_PAYMENT]: {
    [OrderEvent.PAY]: OrderStatus.PENDING_SHIPMENT,
    [OrderEvent.CANCEL]: OrderStatus.CANCELLED
  },
  [OrderStatus.PENDING_SHIPMENT]: {
    [OrderEvent.SHIP]: OrderStatus.PENDING_RECEIPT
  },
  [OrderStatus.PENDING_RECEIPT]: {
    [OrderEvent.CONFIRM_RECEIPT]: OrderStatus.COMPLETED
  },
  [OrderStatus.COMPLETED]: {
    [OrderEvent.APPLY_REFUND]: OrderStatus.REFUNDING
  },
  [OrderStatus.REFUNDING]: {
    [OrderEvent.AGREE_REFUND]: OrderStatus.REFUNDED
  },
  [OrderStatus.CANCELLED]: {},
  [OrderStatus.REFUNDED]: {}
}

/**
 * 判断某状态下能否执行某事件
 */
export function canTransition(status, event) {
  const map = TRANSITIONS[status]
  if (!map) return false
  return Object.prototype.hasOwnProperty.call(map, event)
}

/**
 * 执行状态流转，返回新状态；非法流转抛错，避免脏状态写入。
 */
export function transition(status, event) {
  if (!canTransition(status, event)) {
    throw new Error(
      `非法订单流转: 状态[${statusText(status)}] 不支持事件[${event}]`
    )
  }
  return TRANSITIONS[status][event]
}

/**
 * 获取某状态下当前可执行的事件列表（用于前端按钮渲染）
 */
export function availableEvents(status) {
  return Object.keys(TRANSITIONS[status] || {})
}

export function statusText(status) {
  return OrderStatusText[status] || status
}

// 超时配置
export const PAY_TIMEOUT_MS = 30 * 60 * 1000 // 30 分钟未支付自动取消
export const AUTO_CONFIRM_MS = 15 * 24 * 60 * 60 * 1000 // 发货后 15 天自动确认
