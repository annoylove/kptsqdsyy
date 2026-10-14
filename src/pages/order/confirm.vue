<template>
  <view class="page">
    <view class="addr-card">
      <view class="addr-icon">📍</view>
      <view class="addr-body">
        <view class="addr-line1">
          <text class="addr-name">山间来客</text>
          <text class="addr-phone">138****8888</text>
        </view>
        <text class="addr-detail">浙江省杭州市西湖区文三路 100 号 数码大厦 A 座</text>
      </view>
    </view>

    <view class="goods-card">
      <view class="goods-item" v-for="item in items" :key="item.skuId">
        <image class="g-img" :src="item.cover" mode="aspectFill" />
        <view class="g-body">
          <text class="g-title">{{ item.title }}</text>
          <text class="g-spec">{{ item.specText }}</text>
          <view class="g-foot">
            <text class="g-price">¥{{ item.price }}</text>
            <text class="g-qty">×{{ item.quantity }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="pay-card">
      <view class="pay-row">
        <text>商品金额</text>
        <text>¥{{ totalAmount.toFixed(2) }}</text>
      </view>
      <view class="pay-row">
        <text>运费</text>
        <text>¥0.00</text>
      </view>
      <view class="pay-row total-row">
        <text>实付款</text>
        <text class="total-price">¥{{ totalAmount.toFixed(2) }}</text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view class="channel-card">
      <view
        class="channel"
        v-for="c in channels"
        :key="c.value"
        @tap="channel = c.value"
      >
        <text class="ch-icon">{{ c.icon }}</text>
        <text class="ch-name">{{ c.name }}</text>
        <text class="ch-radio" :class="{ on: channel === c.value }">
          {{ channel === c.value ? '●' : '○' }}
        </text>
      </view>
    </view>

    <view class="submit-bar safe-area-bottom">
      <view class="submit-info">
        <text>应付：</text>
        <text class="submit-price">¥{{ totalAmount.toFixed(2) }}</text>
      </view>
      <view class="submit-btn press-effect" :class="{ dis: submitting }" @tap="submit">
        {{ submitting ? '提交中…' : '提交订单' }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useOrderStore } from '@/store/order'
import { triggerPay } from '@/utils/payRouter'

const orderStore = useOrderStore()
const items = ref([])
const from = ref('cart')
const submitting = ref(false)
const channel = ref('WECHAT')

const channels = [
  { value: 'WECHAT', name: '微信支付', icon: '💚' },
  { value: 'ALIPAY', name: '支付宝', icon: '💙' }
]

const totalAmount = computed(() =>
  items.value.reduce((s, i) => s + i.price * i.quantity, 0)
)

onLoad((query) => {
  from.value = query.from || 'buy'
  const stored =
    from.value === 'cart'
      ? uni.getStorageSync('checkout_items')
      : uni.getStorageSync('direct_buy')
  items.value = stored || []
})

async function submit() {
  if (submitting.value || !items.value.length) return
  submitting.value = true
  try {
    // 1. 创建订单（待付款）
    const order = await orderStore.createOrder(items.value)
    // 2. 支付路由分发（按平台走不同渠道）
    try {
      await triggerPay(order.orderNo, totalAmount.value, { channel: channel.value })
      // 3. 支付成功 -> 状态机流转到待发货
      await orderStore.fire(order.orderNo, 'PAY')
      uni.showToast({ title: '支付成功', icon: 'success' })
      // 购物车来源则移除已购商品
      if (from.value === 'cart') {
        const { useCartStore } = await import('@/store/cart')
        useCartStore().clearSelected()
      }
      uni.removeStorageSync('direct_buy')
      setTimeout(() => {
        uni.redirectTo({ url: `/pages/order/detail?orderNo=${order.orderNo}` })
      }, 600)
    } catch (payErr) {
      // 支付失败/取消：保留待付款订单
      uni.showToast({ title: '支付未完成，可在订单列表中继续支付', icon: 'none' })
      setTimeout(() => {
        uni.redirectTo({ url: '/pages/order/list' })
      }, 800)
    }
  } catch (e) {
    uni.showToast({ title: e.message || '下单失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-color;
  padding: 20rpx 24rpx 160rpx;
}
.addr-card {
  position: relative;
  display: flex;
  gap: 20rpx;
  align-items: center;
  background: $bg-white;
  border-radius: $radius-lg;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
  overflow: hidden;
}
.addr-card::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 8rpx;
  background: $primary-gradient;
}
.addr-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: $primary-light;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  flex-shrink: 0;
}
.addr-body {
  flex: 1;
}
.addr-line1 {
  display: flex;
  gap: 20rpx;
  align-items: baseline;
  margin-bottom: 10rpx;
}
.addr-name {
  font-weight: 700;
  font-size: 30rpx;
}
.addr-phone {
  color: $text-tertiary;
  font-size: 24rpx;
  font-variant-numeric: tabular-nums;
}
.addr-detail {
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.6;
}
.goods-card {
  background: $bg-white;
  border-radius: $radius-lg;
  padding: 8rpx 28rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
}
.goods-item {
  display: flex;
  gap: 20rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $divider-color;
}
.goods-item:last-child {
  border-bottom: none;
}
.g-img {
  width: 140rpx;
  height: 140rpx;
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
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.g-spec {
  font-size: 22rpx;
  color: $text-tertiary;
  background: $bg-gray;
  padding: 4rpx 14rpx;
  border-radius: $radius-pill;
  display: inline-block;
  margin-top: 10rpx;
}
.g-foot {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 16rpx;
}
.g-price {
  color: $price-color;
  font-weight: 700;
  font-size: 30rpx;
  font-variant-numeric: tabular-nums;
}
.g-qty {
  color: $text-tertiary;
  font-size: 24rpx;
}
.pay-card,
.channel-card {
  background: $bg-white;
  border-radius: $radius-lg;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
}
.pay-row {
  display: flex;
  justify-content: space-between;
  font-size: 26rpx;
  color: $text-secondary;
  padding: 12rpx 0;
}
.total-row {
  border-top: 1rpx solid $divider-color;
  margin-top: 12rpx;
  padding-top: 24rpx;
  color: $text-color;
  font-weight: 600;
}
.total-price {
  color: $price-color;
  font-size: 36rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.channel {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 20rpx;
  border-radius: $radius-md;
  transition: background 0.2s ease;
}
.channel:active {
  background: $bg-gray;
}
.ch-icon {
  font-size: 44rpx;
}
.ch-name {
  flex: 1;
  font-size: 28rpx;
  font-weight: 500;
}
.ch-radio {
  color: #d9d9e0;
  font-size: 32rpx;
}
.ch-radio.on {
  color: $primary-color;
}
.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  border-top: 1rpx solid $border-color;
  box-shadow: 0 -4rpx 20rpx rgba(29, 29, 36, 0.05);
}
.submit-info {
  font-size: 26rpx;
  color: $text-secondary;
}
.submit-price {
  color: $price-color;
  font-size: 38rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.submit-btn {
  background: $primary-gradient;
  color: #fff;
  padding: 0 52rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: $radius-pill;
  font-size: 28rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  box-shadow: $shadow-primary;
}
.submit-btn.dis {
  background: $bg-gray;
  color: $text-placeholder;
  box-shadow: none;
}
</style>
