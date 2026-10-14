<template>
  <view class="page">
    <nav-bar title="购物车" :show-back="false" />

    <scroll-view scroll-y class="cart-scroll" :style="{ height: 'calc(100vh - ' + (navHeight + 110) + 'px)' }">
      <!-- 有效商品：按店铺分组 -->
      <view class="shop-group" v-for="group in cartStore.shopGroups" :key="group.shopId">
        <view class="shop-head" @tap="cartStore.toggleShop(group.shopId)">
          <text class="checkbox" :class="{ checked: cartStore.isShopAllSelected(group.shopId) }">
            {{ cartStore.isShopAllSelected(group.shopId) ? '✓' : '' }}
          </text>
          <text class="shop-name">🏪 {{ group.shopName }}</text>
        </view>

        <view class="cart-item" v-for="item in group.items" :key="item.skuId">
          <text
            class="checkbox"
            :class="{ checked: cartStore.selected[item.skuId] }"
            @tap="cartStore.toggleItem(item.skuId)"
          >
            {{ cartStore.selected[item.skuId] ? '✓' : '' }}
          </text>
          <image class="item-img" :src="item.cover" mode="aspectFill" />
          <view class="item-body">
            <text class="item-title">{{ item.title }}</text>
            <text class="item-spec">{{ item.specText }}</text>
            <view class="item-foot">
              <text class="item-price">¥{{ item.price }}</text>
              <!-- 数量步进器（库存边界） -->
              <view class="stepper">
                <text class="step-btn" @tap="changeQty(item, -1)">−</text>
                <text class="step-num">{{ item.quantity }}</text>
                <text class="step-btn" :class="{ dis: item.quantity >= item.stock }" @tap="changeQty(item, 1)">＋</text>
              </view>
            </view>
            <text class="item-subtotal">小计 ¥{{ (item.price * item.quantity).toFixed(2) }}</text>
          </view>
        </view>
      </view>

      <!-- 失效商品分组 -->
      <view class="invalid-group" v-if="cartStore.invalidItems.length">
        <view class="invalid-head">
          <text>失效商品 ({{ cartStore.invalidItems.length }})</text>
          <text class="clear-btn" @tap="clearInvalid">清空失效</text>
        </view>
        <view class="cart-item invalid-item" v-for="item in cartStore.invalidItems" :key="item.skuId">
          <image class="item-img gray" :src="item.cover" mode="aspectFill" />
          <view class="item-body">
            <text class="item-title">{{ item.title }}</text>
            <text class="item-spec">{{ item.specText }}</text>
            <text class="invalid-tag">{{ item.invalidReason || '已失效' }}</text>
          </view>
          <text class="del-btn" @tap="cartStore.removeItem(item.skuId)">删除</text>
        </view>
      </view>

      <view v-if="!cartStore.items.length" class="empty">
        <text>购物车空空如也</text>
        <navigator url="/pages/shop/shop" open-type="switchTab" class="go-shop">去逛逛</navigator>
      </view>
    </scroll-view>

    <!-- 结算栏 -->
    <view class="settle-bar safe-area-bottom" v-if="cartStore.items.length">
      <text class="checkbox all" :class="{ checked: cartStore.isAllSelected }" @tap="cartStore.toggleAll()">
        {{ cartStore.isAllSelected ? '✓' : '' }}
      </text>
      <text class="all-label">全选</text>
      <view class="total">
        <text>合计：</text>
        <text class="total-price">¥{{ cartStore.totalPrice.toFixed(2) }}</text>
      </view>
      <view class="settle-btn press-effect" :class="{ dis: !cartStore.selectedCount }" @tap="checkout">
        结算({{ cartStore.selectedCount }})
      </view>
    </view>
  </view>
</template>

<script setup>
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'

const cartStore = useCartStore()
const userStore = useUserStore()
const systemInfo = uni.getSystemInfoSync()
const navHeight = (systemInfo.statusBarHeight || 0) + 44

onLoad(() => {
  cartStore.refreshValidity()
})
onShow(() => {
  cartStore.refreshValidity()
})

function changeQty(item, delta) {
  cartStore.updateQuantity(item.skuId, item.quantity + delta)
}

function clearInvalid() {
  cartStore.invalidItems.forEach((i) => cartStore.removeItem(i.skuId))
}

/** 结算：先做后端库存二次拦截 */
async function checkout() {
  if (!cartStore.selectedCount) return
  if (!userStore.requireLogin()) return
  const ok = await cartStore.checkStockBeforeCheckout()
  if (!ok) return
  uni.setStorageSync('checkout_items', cartStore.selectedItems)
  uni.navigateTo({ url: '/pages/order/confirm?from=cart' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-color;
}
.cart-scroll {
  padding: 20rpx 24rpx;
}
.shop-group {
  background: $bg-white;
  border-radius: $radius-lg;
  margin-bottom: 20rpx;
  padding: 24rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
}
.shop-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid $divider-color;
}
.shop-name {
  font-size: 26rpx;
  font-weight: 700;
  letter-spacing: 0.5rpx;
}
.checkbox {
  width: 44rpx;
  height: 44rpx;
  border: 2rpx solid #d9d9e0;
  border-radius: 50%;
  text-align: center;
  line-height: 44rpx;
  font-size: 26rpx;
  color: #fff;
  flex-shrink: 0;
  transition: all 0.2s ease;
}
.checkbox.checked {
  background: $primary-gradient;
  border-color: transparent;
  box-shadow: 0 4rpx 10rpx rgba(255, 77, 79, 0.3);
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $divider-color;
}
.cart-item:last-child {
  border-bottom: none;
}
.item-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-md;
  background: $bg-gray;
}
.item-img.gray {
  filter: grayscale(1);
  opacity: 0.5;
}
.item-body {
  flex: 1;
  min-width: 0;
}
.item-title {
  font-size: 26rpx;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-weight: 500;
}
.item-spec {
  font-size: 22rpx;
  color: $text-tertiary;
  background: $bg-gray;
  padding: 6rpx 16rpx;
  border-radius: $radius-pill;
  display: inline-block;
  margin-top: 10rpx;
}
.item-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}
.item-price {
  color: $price-color;
  font-size: 32rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.item-subtotal {
  font-size: 22rpx;
  color: $text-tertiary;
  margin-top: 8rpx;
  display: block;
}
.stepper {
  display: flex;
  align-items: center;
  border: 1rpx solid #e5e5ea;
  border-radius: $radius-pill;
  overflow: hidden;
}
.step-btn {
  width: 56rpx;
  height: 52rpx;
  line-height: 52rpx;
  text-align: center;
  background: $bg-gray;
  font-size: 32rpx;
  color: $text-secondary;
}
.step-btn.dis {
  color: #d1d1d6;
}
.step-num {
  width: 70rpx;
  text-align: center;
  font-size: 26rpx;
  font-weight: 600;
}
.invalid-group {
  background: $bg-white;
  border-radius: $radius-lg;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
}
.invalid-head {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: $text-tertiary;
  padding-bottom: 16rpx;
}
.clear-btn {
  color: $primary-color;
  font-weight: 500;
}
.invalid-item {
  opacity: 0.75;
}
.invalid-tag {
  font-size: 22rpx;
  color: $primary-color;
  margin-top: 10rpx;
}
.del-btn {
  font-size: 24rpx;
  color: $text-tertiary;
  padding: 8rpx;
}
.empty {
  text-align: center;
  padding: 160rpx 0;
  color: $text-tertiary;
}
.go-shop {
  display: inline-block;
  margin-top: 32rpx;
  color: #fff;
  background: $primary-gradient;
  padding: 20rpx 64rpx;
  border-radius: $radius-pill;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: $shadow-primary;
}
.settle-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  gap: 16rpx;
  border-top: 1rpx solid $border-color;
  box-shadow: 0 -4rpx 20rpx rgba(29, 29, 36, 0.05);
}
.all-label {
  font-size: 26rpx;
}
.total {
  margin-left: auto;
  font-size: 26rpx;
  color: $text-secondary;
}
.total-price {
  color: $price-color;
  font-size: 36rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.settle-btn {
  background: $primary-gradient;
  color: #fff;
  padding: 0 44rpx;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: $radius-pill;
  font-size: 28rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  box-shadow: $shadow-primary;
}
.settle-btn.dis {
  background: $bg-gray;
  color: $text-placeholder;
  box-shadow: none;
}
</style>
