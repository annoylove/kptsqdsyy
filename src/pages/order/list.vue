<template>
  <view class="page">
    <!-- 状态标签页 -->
    <scroll-view scroll-x class="tabs">
      <view
        class="tab"
        :class="{ active: currentTab === t.value }"
        v-for="t in tabs"
        :key="t.value"
        @tap="switchTab(t.value)"
      >
        {{ t.label }}
      </view>
    </scroll-view>

    <scroll-view scroll-y class="list">
      <view class="order-card press-effect" v-for="o in filteredOrders" :key="o.orderNo" @tap="goDetail(o.orderNo)">
        <view class="order-head">
          <text class="order-no">{{ o.orderNo }}</text>
          <text class="order-status" :class="statusClass(o.status)">{{ statusText(o.status) }}</text>
        </view>
        <view class="order-goods" v-for="item in o.items" :key="item.skuId">
          <image class="o-img" :src="item.cover" mode="aspectFill" />
          <view class="o-body">
            <text class="o-title">{{ item.title }}</text>
            <text class="o-spec">{{ item.specText }} ×{{ item.quantity }}</text>
          </view>
          <text class="o-price">¥{{ item.price }}</text>
        </view>
        <view class="order-foot">
          <text class="o-total">共{{ totalQty(o) }}件 合计 <text class="price">¥{{ o.totalAmount.toFixed(2) }}</text></text>
          <view class="actions">
            <template v-if="o.status === 'PENDING_PAYMENT'">
              <text class="btn primary" @tap.stop="pay(o)">去支付 {{ payCountdown(o) }}</text>
              <text class="btn" @tap.stop="cancel(o)">取消</text>
            </template>
            <template v-else-if="o.status === 'PENDING_SHIPMENT'">
              <text class="btn" @tap.stop="ship(o)">模拟发货</text>
            </template>
            <template v-else-if="o.status === 'PENDING_RECEIPT'">
              <text class="btn primary" @tap.stop="confirm(o)">确认收货</text>
            </template>
            <template v-else-if="o.status === 'COMPLETED'">
              <text class="btn" @tap.stop="applyRefund(o)">申请售后</text>
            </template>
            <template v-else-if="o.status === 'REFUNDING'">
              <text class="btn" @tap.stop="agreeRefund(o)">商家同意退款</text>
            </template>
          </view>
        </view>
      </view>

      <view v-if="!filteredOrders.length" class="empty">
        <text>暂无订单</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onHide } from '@dcloudio/uni-app'
import { useOrderStore } from '@/store/order'
import { OrderStatusText, statusText, PAY_TIMEOUT_MS } from '@/utils/orderStateMachine'
import { triggerPay } from '@/utils/payRouter'
import { countdown } from '@/utils/format'

const orderStore = useOrderStore()
const currentTab = ref('')
const now = ref(Date.now())
let tickTimer = null

const tabs = [
  { value: '', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待付款' },
  { value: 'PENDING_SHIPMENT', label: '待发货' },
  { value: 'PENDING_RECEIPT', label: '待收货' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'REFUNDING', label: '售后' }
]

const filteredOrders = computed(() =>
  currentTab.value ? orderStore.orders.filter((o) => o.status === currentTab.value) : orderStore.orders
)

function switchTab(v) {
  currentTab.value = v
}

function statusClass(s) {
  return {
    'st-pay': s === 'PENDING_PAYMENT',
    'st-done': s === 'COMPLETED' || s === 'REFUNDED'
  }
}

function totalQty(o) {
  return o.items.reduce((s, i) => s + i.quantity, 0)
}

/** 待付款倒计时（超时由 store 定时器自动取消） */
function payCountdown(o) {
  const remain = o.createTime + PAY_TIMEOUT_MS - now.value
  return remain > 0 ? countdown(remain) : '超时'
}

async function pay(o) {
  try {
    await triggerPay(o.orderNo, o.totalAmount)
    await orderStore.fire(o.orderNo, 'PAY')
    uni.showToast({ title: '支付成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '支付失败', icon: 'none' })
  }
}

async function act(o, event, extra) {
  try {
    await orderStore.fire(o.orderNo, event, extra)
    uni.showToast({ title: '操作成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

const cancel = (o) => act(o, 'CANCEL')
const ship = (o) => act(o, 'SHIP', { trackingNo: 'SF' + Date.now() })
const confirm = (o) => act(o, 'CONFIRM_RECEIPT')
const applyRefund = (o) => act(o, 'APPLY_REFUND')
const agreeRefund = (o) => act(o, 'AGREE_REFUND')

function goDetail(orderNo) {
  uni.navigateTo({ url: `/pages/order/detail?orderNo=${orderNo}` })
}

onLoad((query) => {
  if (query.status) currentTab.value = query.status
})

onShow(async () => {
  await orderStore.fetchOrders().catch(() => {})
  tickTimer = setInterval(() => {
    now.value = Date.now()
    orderStore._tick()
  }, 1000)
})
onHide(() => clearInterval(tickTimer))
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg-color;
}
.tabs {
  white-space: nowrap;
  background: $bg-white;
  flex-shrink: 0;
  padding: 8rpx 16rpx;
  border-bottom: 1rpx solid $border-color;
}
.tab {
  display: inline-block;
  padding: 14rpx 32rpx;
  font-size: 26rpx;
  color: $text-secondary;
  border-radius: $radius-pill;
  margin: 8rpx 4rpx;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.tab.active {
  color: #fff;
  font-weight: 600;
  background: $primary-gradient;
  box-shadow: 0 4rpx 12rpx rgba(255, 77, 79, 0.3);
}
.list {
  flex: 1;
  padding: 20rpx 24rpx;
}
.order-card {
  background: $bg-white;
  border-radius: $radius-lg;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
}
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid $divider-color;
}
.order-no {
  font-size: 24rpx;
  color: $text-tertiary;
  font-variant-numeric: tabular-nums;
}
.order-status {
  font-size: 22rpx;
  font-weight: 600;
  padding: 8rpx 24rpx;
  border-radius: $radius-pill;
  background: $bg-gray;
  color: $text-secondary;
}
.st-pay {
  color: $warning-color;
  background: rgba(255, 159, 10, 0.1);
}
.st-done {
  color: $success-color;
  background: rgba(52, 199, 89, 0.1);
}
.order-goods {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 0;
}
.o-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-md;
  background: $bg-gray;
}
.o-body {
  flex: 1;
  min-width: 0;
}
.o-title {
  font-size: 26rpx;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.o-spec {
  font-size: 22rpx;
  color: $text-tertiary;
  margin-top: 6rpx;
}
.o-price {
  font-size: 26rpx;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 20rpx;
  border-top: 1rpx solid $divider-color;
  flex-wrap: wrap;
  gap: 12rpx;
}
.o-total {
  font-size: 24rpx;
  color: $text-secondary;
}
.o-total .price {
  color: $price-color;
  font-size: 30rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.actions {
  display: flex;
  gap: 16rpx;
}
.btn {
  padding: 12rpx 32rpx;
  border: 1rpx solid #e0e0e6;
  border-radius: $radius-pill;
  font-size: 24rpx;
  color: $text-secondary;
  background: $bg-white;
  transition: all 0.2s ease;
}
.btn.primary {
  border: none;
  color: #fff;
  background: $primary-gradient;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(255, 77, 79, 0.25);
}
.empty {
  text-align: center;
  padding: 160rpx 0;
  color: $text-tertiary;
}
</style>
