<template>
  <view class="page">
    <nav-bar title="我的" :show-back="false" theme="white" background-color="transparent" :fixed="false" />

    <view class="header" :style="{ paddingTop: navHeight + 'px' }">
      <view class="header-deco deco-1"></view>
      <view class="header-deco deco-2"></view>
      <view class="user-row" v-if="userStore.isLogin">
        <image class="avatar" :src="userStore.user.avatar" />
        <view class="user-info">
          <text class="nick">{{ userStore.user.nickname }}</text>
          <text class="phone">{{ userStore.user.phone }}</text>
        </view>
      </view>
      <view class="user-row press-effect" v-else @tap="goLogin">
        <view class="avatar placeholder">👤</view>
        <view class="user-info">
          <text class="nick">点击登录</text>
          <text class="phone">登录后查看订单与收藏</text>
        </view>
      </view>
    </view>

    <!-- 订单状态入口 -->
    <view class="order-card">
      <view class="card-head">
        <text class="title">我的订单</text>
        <text class="more" @tap="goOrders('')">全部订单 ›</text>
      </view>
      <view class="order-grid">
        <view class="grid-item press-effect" v-for="s in orderEntries" :key="s.status" @tap="goOrders(s.status)">
          <text class="icon">{{ s.icon }}</text>
          <text class="label">{{ s.label }}</text>
          <text class="badge" v-if="orderStore.countByStatus[s.status]">
            {{ orderStore.countByStatus[s.status] }}
          </text>
        </view>
      </view>
    </view>

    <view class="menu-card">
      <view class="menu-item press-effect" @tap="goOrders('')">
        <view class="menu-left">
          <view class="menu-icon" style="background: #fff1f0">📦</view>
          <text>我的订单</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item press-effect" @tap="showTip">
        <view class="menu-left">
          <view class="menu-icon" style="background: #fff0f6">❤️</view>
          <text>我的收藏</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item press-effect" @tap="showTip">
        <view class="menu-left">
          <view class="menu-icon" style="background: #e8f4ff">📍</view>
          <text>收货地址</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item press-effect" @tap="showTip">
        <view class="menu-left">
          <view class="menu-icon" style="background: #f2f3f5">⚙️</view>
          <text>设置</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="logout press-effect" v-if="userStore.isLogin" @tap="logout">退出登录</view>
  </view>
</template>

<script setup>
import { useUserStore } from '@/store/user'
import { useOrderStore } from '@/store/order'

const userStore = useUserStore()
const orderStore = useOrderStore()

const systemInfo = uni.getSystemInfoSync()
const navHeight = (systemInfo.statusBarHeight || 0) + 44

const orderEntries = [
  { status: 'PENDING_PAYMENT', label: '待付款', icon: '💰' },
  { status: 'PENDING_SHIPMENT', label: '待发货', icon: '📦' },
  { status: 'PENDING_RECEIPT', label: '待收货', icon: '🚚' },
  { status: 'REFUNDING', label: '退款/售后', icon: '↩️' }
]

function goLogin() {
  uni.navigateTo({ url: '/pages/login/login' })
}
function goOrders(status) {
  // 未登录时点击订单入口 -> 跳转登录页
  if (!userStore.requireLogin()) return
  uni.navigateTo({ url: `/pages/order/list${status ? '?status=' + status : ''}` })
}
function showTip() {
  uni.showToast({ title: '演示功能', icon: 'none' })
}
function logout() {
  userStore.logout()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-color;
}
.header {
  position: relative;
  background: linear-gradient(160deg, #ff6a6c 0%, #ff4d4f 60%, #f5384d 100%);
  padding-bottom: 100rpx;
  overflow: hidden;
}
.header-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}
.deco-1 {
  width: 320rpx;
  height: 320rpx;
  right: -100rpx;
  top: -80rpx;
}
.deco-2 {
  width: 200rpx;
  height: 200rpx;
  right: 120rpx;
  bottom: -60rpx;
  background: rgba(255, 255, 255, 0.07);
}
.user-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 32rpx 32rpx;
}
.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15);
}
.avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
}
.nick {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
  display: block;
  letter-spacing: 1rpx;
}
.phone {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 1rpx;
}
.order-card {
  background: $bg-white;
  border-radius: $radius-lg;
  margin: -64rpx 24rpx 20rpx;
  padding: 28rpx 24rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
  position: relative;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28rpx;
}
.title {
  font-size: 30rpx;
  font-weight: 700;
  letter-spacing: 0.5rpx;
}
.more {
  font-size: 24rpx;
  color: $text-tertiary;
}
.order-grid {
  display: flex;
}
.grid-item {
  flex: 1;
  text-align: center;
  position: relative;
  padding: 8rpx 0;
}
.icon {
  font-size: 48rpx;
  display: block;
  margin-bottom: 8rpx;
}
.label {
  font-size: 24rpx;
  color: $text-secondary;
}
.badge {
  position: absolute;
  top: -4rpx;
  right: 30rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  background: $primary-gradient;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  padding: 0 6rpx;
  box-shadow: 0 4rpx 10rpx rgba(255, 77, 79, 0.35);
}
.menu-card {
  background: $bg-white;
  border-radius: $radius-lg;
  margin: 0 24rpx;
  padding: 0 28rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
  overflow: hidden;
}
.menu-left {
  display: flex;
  align-items: center;
  gap: 24rpx;
}
.menu-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
}
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $divider-color;
  font-size: 28rpx;
  letter-spacing: 0.5rpx;
}
.menu-item:last-child {
  border-bottom: none;
}
.arrow {
  color: #d1d1d6;
  font-size: 32rpx;
}
.logout {
  margin: 40rpx 24rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: $bg-white;
  border-radius: $radius-pill;
  color: $primary-color;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
}
</style>
