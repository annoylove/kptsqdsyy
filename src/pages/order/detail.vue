<template>
  <view class="page" v-if="order">
    <!-- 状态头 -->
    <view class="status-head">
      <text class="status-name">{{ statusText(order.status) }}</text>
      <text class="status-tip">{{ statusTip }}</text>
    </view>

    <!-- 状态机流转时间轴 -->
    <view class="timeline-card">
      <text class="card-title">订单流转</text>
      <view class="timeline">
        <view
          class="tl-node"
          v-for="(step, i) in timeline"
          :key="step.status"
          :class="{ reached: step.reached, current: step.current }"
        >
          <view class="tl-dot">
            <text v-if="step.reached">✓</text>
          </view>
          <view class="tl-line" v-if="i < timeline.length - 1" :class="{ on: step.reached && timeline[i + 1].reached }"></view>
          <text class="tl-label">{{ step.label }}</text>
          <text class="tl-time" v-if="step.time">{{ step.time }}</text>
        </view>
      </view>
    </view>

    <!-- 物流信息 -->
    <view class="info-card" v-if="order.trackingNo">
      <text class="card-title">物流信息</text>
      <view class="info-row">
        <text>运单号</text>
        <text>{{ order.trackingNo }}</text>
      </view>
    </view>

    <!-- 商品清单 -->
    <view class="info-card">
      <text class="card-title">商品清单</text>
      <view class="goods-item" v-for="item in order.items" :key="item.skuId">
        <image class="g-img" :src="item.cover" mode="aspectFill" />
        <view class="g-body">
          <text class="g-title">{{ item.title }}</text>
          <text class="g-spec">{{ item.specText }} ×{{ item.quantity }}</text>
        </view>
        <text class="g-price">¥{{ (item.price * item.quantity).toFixed(2) }}</text>
      </view>
      <view class="total-row">
        <text>实付金额</text>
        <text class="total-price">¥{{ order.totalAmount.toFixed(2) }}</text>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="info-card">
      <text class="card-title">订单信息</text>
      <view class="info-row"><text>订单编号</text><text>{{ order.orderNo }}</text></view>
      <view class="info-row"><text>下单时间</text><text>{{ fmt(order.createTime) }}</text></view>
      <view class="info-row" v-if="order.payTime"><text>支付时间</text><text>{{ fmt(order.payTime) }}</text></view>
    </view>

    <!-- 底部操作（由状态机 availableEvents 驱动） -->
    <view class="bottom-bar safe-area-bottom">
      <view class="btn press-effect" v-for="ev in actions" :key="ev.event" :class="{ primary: ev.primary }" @tap="doAction(ev.event)">
        {{ ev.label }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useOrderStore } from '@/store/order'
import {
  OrderStatus,
  OrderEvent,
  statusText,
  availableEvents,
  PAY_TIMEOUT_MS
} from '@/utils/orderStateMachine'
import { triggerPay } from '@/utils/payRouter'
import { countdown } from '@/utils/format'

const orderStore = useOrderStore()
const order = ref(null)

onLoad((query) => {
  order.value = orderStore.orders.find((o) => o.orderNo === query.orderNo) || null
})

const statusTip = computed(() => {
  if (!order.value) return ''
  switch (order.value.status) {
    case OrderStatus.PENDING_PAYMENT:
      return '请在 30 分钟内完成支付，超时订单自动取消'
    case OrderStatus.PENDING_SHIPMENT:
      return '商家备货中，请耐心等待'
    case OrderStatus.PENDING_RECEIPT:
      return '发货后 15 天将自动确认收货'
    case OrderStatus.COMPLETED:
      return '交易成功，资金已结算给商家'
    case OrderStatus.CANCELLED:
      return order.value.cancelReason || '订单已取消'
    case OrderStatus.REFUNDING:
      return '等待商家处理退款申请'
    case OrderStatus.REFUNDED:
      return '退款已原路退回'
    default:
      return ''
  }
})

/** 时间轴：正常链路 + 售后/取消分支 */
const timeline = computed(() => {
  if (!order.value) return []
  const s = order.value.status
  const o = order.value
  const steps = [
    { status: OrderStatus.PENDING_PAYMENT, label: '待付款', reached: true, time: fmt(o.createTime), current: s === OrderStatus.PENDING_PAYMENT },
    { status: OrderStatus.PENDING_SHIPMENT, label: '待发货', reached: reached(s, OrderStatus.PENDING_SHIPMENT), time: o.payTime ? fmt(o.payTime) : '', current: s === OrderStatus.PENDING_SHIPMENT },
    { status: OrderStatus.PENDING_RECEIPT, label: '待收货', reached: reached(s, OrderStatus.PENDING_RECEIPT), time: o.shipTime ? fmt(o.shipTime) : '', current: s === OrderStatus.PENDING_RECEIPT },
    { status: OrderStatus.COMPLETED, label: '已完成', reached: reached(s, OrderStatus.COMPLETED), time: o.finishTime ? fmt(o.finishTime) : '', current: s === OrderStatus.COMPLETED }
  ]
  if (s === OrderStatus.CANCELLED) {
    return steps.slice(0, 1).concat([{ status: s, label: '已取消', reached: true, current: true, time: fmt(Date.now()) }])
  }
  if (s === OrderStatus.REFUNDING || s === OrderStatus.REFUNDED) {
    return steps.concat([
      { status: OrderStatus.REFUNDING, label: '售后中', reached: true, current: s === OrderStatus.REFUNDING, time: '' },
      { status: OrderStatus.REFUNDED, label: '已退款', reached: s === OrderStatus.REFUNDED, current: s === OrderStatus.REFUNDED, time: s === OrderStatus.REFUNDED ? fmt(Date.now()) : '' }
    ])
  }
  return steps
})

/** 判断某状态在正常链路中是否已到达 */
function reached(current, target) {
  const chain = [
    OrderStatus.PENDING_PAYMENT,
    OrderStatus.PENDING_SHIPMENT,
    OrderStatus.PENDING_RECEIPT,
    OrderStatus.COMPLETED
  ]
  const terminal = [OrderStatus.CANCELLED, OrderStatus.REFUNDING, OrderStatus.REFUNDED]
  if (terminal.includes(current)) {
    // 售后分支：已完成链路视为走过；取消只走过待付款
    if (current === OrderStatus.CANCELLED) return chain.indexOf(target) < 1
    return chain.indexOf(target) <= 3
  }
  return chain.indexOf(current) >= chain.indexOf(target)
}

const ACTION_MAP = {
  [OrderEvent.PAY]: { label: '立即支付', primary: true },
  [OrderEvent.CANCEL]: { label: '取消订单', primary: false },
  [OrderEvent.SHIP]: { label: '模拟发货', primary: false },
  [OrderEvent.CONFIRM_RECEIPT]: { label: '确认收货', primary: true },
  [OrderEvent.APPLY_REFUND]: { label: '申请售后', primary: false },
  [OrderEvent.AGREE_REFUND]: { label: '商家同意退款', primary: true }
}

/** 按钮完全由状态机 availableEvents 驱动，杜绝非法操作入口 */
const actions = computed(() => {
  if (!order.value) return []
  return availableEvents(order.value.status).map((ev) => ({ event: ev, ...ACTION_MAP[ev] }))
})

async function doAction(event) {
  const o = order.value
  if (event === OrderEvent.PAY) {
    try {
      await triggerPay(o.orderNo, o.totalAmount)
    } catch (e) {
      uni.showToast({ title: '支付取消', icon: 'none' })
      return
    }
  }
  try {
    await orderStore.fire(o.orderNo, event, event === OrderEvent.SHIP ? { trackingNo: 'SF' + Date.now() } : {})
    uni.showToast({ title: '操作成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message, icon: 'none' })
  }
}

function fmt(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const p = (n) => (n < 10 ? '0' + n : n)
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: 160rpx;
}
.status-head {
  position: relative;
  background: linear-gradient(160deg, #ff6a6c 0%, #ff4d4f 60%, #f5384d 100%);
  padding: 48rpx 32rpx 80rpx;
  color: #fff;
  overflow: hidden;
}
.status-head::after {
  content: '';
  position: absolute;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  right: -100rpx;
  top: -120rpx;
}
.status-name {
  font-size: 44rpx;
  font-weight: 800;
  display: block;
  letter-spacing: 1rpx;
  position: relative;
}
.status-tip {
  font-size: 24rpx;
  opacity: 0.9;
  margin-top: 16rpx;
  display: block;
  position: relative;
}
.timeline-card {
  background: $bg-white;
  border-radius: $radius-lg;
  margin: -48rpx 24rpx 20rpx;
  padding: 28rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
  position: relative;
}
.card-title {
  font-size: 28rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 28rpx;
  letter-spacing: 0.5rpx;
}
.timeline {
  display: flex;
  justify-content: space-between;
  position: relative;
}
.tl-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex: 1;
}
.tl-dot {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: $bg-gray;
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  transition: all 0.3s ease;
}
.tl-node.reached .tl-dot {
  background: $primary-gradient;
  box-shadow: 0 4rpx 10rpx rgba(255, 77, 79, 0.3);
}
.tl-node.current .tl-dot {
  box-shadow: 0 0 0 8rpx rgba(255, 77, 79, 0.15);
}
.tl-line {
  position: absolute;
  top: 22rpx;
  left: 50%;
  width: 100%;
  height: 4rpx;
  background: #ececf0;
  z-index: 1;
}
.tl-line.on {
  background: $primary-color;
}
.tl-label {
  font-size: 24rpx;
  margin-top: 16rpx;
  color: $text-tertiary;
}
.tl-node.reached .tl-label {
  color: $text-color;
  font-weight: 600;
}
.tl-time {
  font-size: 20rpx;
  color: $text-placeholder;
  margin-top: 6rpx;
  font-variant-numeric: tabular-nums;
}
.info-card {
  background: $bg-white;
  border-radius: $radius-lg;
  margin: 0 24rpx 20rpx;
  padding: 28rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
}
.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 26rpx;
  color: $text-secondary;
  padding: 12rpx 0;
}
.goods-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 0;
}
.g-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-md;
  background: $bg-gray;
}
.g-body {
  flex: 1;
  min-width: 0;
}
.g-title {
  font-size: 26rpx;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.g-spec {
  font-size: 22rpx;
  color: $text-tertiary;
  margin-top: 6rpx;
}
.g-price {
  font-size: 26rpx;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1rpx solid $divider-color;
  padding-top: 24rpx;
  margin-top: 12rpx;
  font-size: 26rpx;
}
.total-price {
  color: $price-color;
  font-size: 36rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid $border-color;
  box-shadow: 0 -4rpx 20rpx rgba(29, 29, 36, 0.05);
}
.btn {
  padding: 16rpx 40rpx;
  border: 1rpx solid #e0e0e6;
  border-radius: $radius-pill;
  font-size: 26rpx;
  color: $text-secondary;
  background: $bg-white;
}
.btn.primary {
  border: none;
  color: #fff;
  background: $primary-gradient;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(255, 77, 79, 0.25);
}
</style>
