/**
 * 订单 Store：状态机驱动 + 超时自动流转
 * - 待付款超过 30 分钟 -> 自动取消
 * - 发货后超过 15 天 -> 自动确认收货
 */
import { defineStore } from 'pinia'
import { orderApi } from '@/api'
import { useUserStore } from './user'
import {
  OrderStatus,
  OrderEvent,
  transition,
  canTransition,
  PAY_TIMEOUT_MS,
  AUTO_CONFIRM_MS
} from '@/utils/orderStateMachine'

let timer = null

export const useOrderStore = defineStore('order', {
  state: () => ({
    orders: uni.getStorageSync('order_list') || [],
    loading: false
  }),

  getters: {
    /** 按状态取订单 */
    ordersByStatus: (state) => (status) =>
      status ? state.orders.filter((o) => o.status === status) : state.orders,
    /** 待付款订单剩余支付时间 ms */
    payRemaining: () => (order) =>
      order.createTime + PAY_TIMEOUT_MS - Date.now(),
    /** 各状态订单数（用于"我的"页角标） */
    countByStatus: (state) => {
      const map = {}
      Object.values(OrderStatus).forEach((s) => {
        map[s] = state.orders.filter((o) => o.status === s).length
      })
      return map
    }
  },

  actions: {
    _persist() {
      uni.setStorageSync('order_list', this.orders)
    },

    /** 创建订单 */
    async createOrder(items) {
      this.loading = true
      try {
        const res = await orderApi.create({ items })
        const order = res.data
        this.orders.unshift(order)
        this._persist()
        return order
      } finally {
        this.loading = false
      }
    },

    /**
     * 统一状态流转入口：先本地状态机校验，再请求后端
     * @param {string} orderNo
     * @param {string} event OrderEvent
     * @param {object} extra 附加参数（如物流单号）
     */
    async fire(orderNo, event, extra = {}) {
      const order = this.orders.find((o) => o.orderNo === orderNo)
      if (!order) throw new Error('订单不存在')
      // 本地状态机校验（非法流转直接拦截）
      if (!canTransition(order.status, event)) {
        throw new Error(`当前状态不允许该操作`)
      }
      let res
      switch (event) {
        case OrderEvent.PAY:
          res = await orderApi.pay(orderNo)
          break
        case OrderEvent.CANCEL:
          res = await orderApi.cancel(orderNo)
          break
        case OrderEvent.SHIP:
          res = await orderApi.ship(orderNo, extra.trackingNo || 'SF' + Date.now())
          break
        case OrderEvent.CONFIRM_RECEIPT:
          res = await orderApi.confirm(orderNo)
          break
        case OrderEvent.APPLY_REFUND:
          res = await orderApi.applyRefund(orderNo)
          break
        case OrderEvent.AGREE_REFUND:
          res = await orderApi.agreeRefund(orderNo)
          break
        default:
          throw new Error('未知事件')
      }
      // 以后端返回为准（后端同样经过状态机校验）
      Object.assign(order, res.data)
      this._persist()
      return order
    },

    /** 从后端同步订单（未登录则清空本地缓存，避免显示上次登录的订单） */
    async fetchOrders() {
      if (!useUserStore().isLogin) {
        this.orders = []
        this._persist()
        return
      }
      const res = await orderApi.list({})
      this.orders = res.data.list
      this._persist()
    },

    /** 清空订单缓存（登出时调用） */
    clear() {
      this.orders = []
      uni.removeStorageSync('order_list')
    },

    /**
     * 启动定时器：每分钟检查一次超时订单
     * - 待付款超时 -> 取消
     * - 待收货超时 -> 自动确认
     */
    startTimers() {
      if (timer) clearInterval(timer)
      timer = setInterval(() => this._tick(), 60 * 1000)
      this._tick()
    },

    stopTimers() {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    },

    async _tick() {
      let changed = false
      for (const order of this.orders) {
        if (
          order.status === OrderStatus.PENDING_PAYMENT &&
          Date.now() - order.createTime > PAY_TIMEOUT_MS &&
          canTransition(order.status, OrderEvent.CANCEL)
        ) {
          order.status = transition(order.status, OrderEvent.CANCEL)
          order.cancelReason = '超时未支付，自动取消'
          changed = true
        } else if (
          order.status === OrderStatus.PENDING_RECEIPT &&
          order.shipTime &&
          Date.now() - order.shipTime > AUTO_CONFIRM_MS &&
          canTransition(order.status, OrderEvent.CONFIRM_RECEIPT)
        ) {
          order.status = transition(order.status, OrderEvent.CONFIRM_RECEIPT)
          order.finishTime = Date.now()
          order.confirmType = 'auto'
          changed = true
        }
      }
      if (changed) this._persist()
    }
  }
})
