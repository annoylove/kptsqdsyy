<template>
  <view class="page">
    <nav-bar :title="goods.title || '商品详情'" />

    <scroll-view scroll-y class="detail-scroll" v-if="goods.id" :style="{ height: 'calc(100vh - ' + navHeight + 'px)' }">
      <!-- 商品轮播图 -->
      <swiper class="banner" indicator-dots circular>
        <swiper-item v-for="(img, i) in goods.images" :key="i">
          <image class="banner-img" :src="img" mode="aspectFill" />
        </swiper-item>
      </swiper>

      <view class="price-block">
        <text class="price-symbol">¥</text>
        <text class="price-num">{{ goods.price }}</text>
        <text class="sales">已售 {{ goods.sales }} 件</text>
      </view>

      <view class="title-block">
        <text class="goods-title">{{ goods.title }}</text>
      </view>

      <view class="shop-block">
        <text class="shop-name">{{ shopName }}</text>
        <text class="shop-enter">进店 ›</text>
      </view>

      <!-- SKU 预览行：点击唤起面板 -->
      <view class="spec-row" @tap="skuVisible = true">
        <text class="spec-label">规格</text>
        <text class="spec-value">{{ selectedSpecText || '请选择 ' + specNames }}</text>
        <text class="arrow">›</text>
      </view>

      <view class="desc-block">
        <text class="desc-title">商品详情</text>
        <text class="desc-text">
          本商品为 Mock 演示数据。支持多规格 SKU 选择、实时价格库存联动、库存边界校验。
          点击下方"加入购物车"或"立即购买"唤起 SKU 面板体验完整交互。
        </text>
      </view>
    </scroll-view>

    <!-- 底部操作栏（含 iOS 安全区） -->
    <view class="bottom-bar safe-area-bottom" v-if="goods.id">
      <view class="bar-icon press-effect" @tap="goCart">
        <text class="icon">🛒</text>
        <text class="badge" v-if="cartStore.badgeCount">{{ cartStore.badgeCount }}</text>
      </view>
      <view class="bar-btn cart-btn press-effect" @tap="openSku('cart')">加入购物车</view>
      <view class="bar-btn buy-btn press-effect" @tap="openSku('buy')">立即购买</view>
    </view>

    <!-- SKU 面板 -->
    <sku-panel
      v-model:visible="skuVisible"
      :goods="goods"
      :mode="skuMode"
      @confirm="onSkuConfirm"
    />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { goodsApi } from '@/api'
import { useCartStore } from '@/store/cart'
import { useUserStore } from '@/store/user'

const cartStore = useCartStore()
const userStore = useUserStore()

const goods = ref({})
const shops = ref([])
const skuVisible = ref(false)
const skuMode = ref('cart')
const selectedSpecText = ref('')

const systemInfo = uni.getSystemInfoSync()
const navHeight = (systemInfo.statusBarHeight || 0) + 44

const shopName = computed(() => {
  const s = shops.value.find((x) => x.id === goods.value.shopId)
  return s ? s.name : ''
})
const specNames = computed(() => (goods.value.specs || []).map((s) => s.name).join('/'))

onLoad(async (query) => {
  const [goodsRes, shopRes] = await Promise.all([
    goodsApi.detail(query.id),
    goodsApi.shops()
  ])
  goods.value = goodsRes.data
  shops.value = shopRes.data
})

function openSku(mode) {
  skuMode.value = mode
  skuVisible.value = true
}

/** SKU 面板确认 -> 加购 / 立即购买 */
function onSkuConfirm({ sku, quantity, specText }) {
  selectedSpecText.value = specText
  if (!userStore.requireLogin()) return

  if (skuMode.value === 'cart') {
    cartStore.addItem(
      {
        goodsId: goods.value.id,
        skuId: sku.id,
        shopId: goods.value.shopId,
        shopName: shopName.value,
        title: goods.value.title,
        cover: goods.value.cover,
        specText,
        price: sku.price,
        stock: sku.stock
      },
      quantity
    )
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } else {
    // 立即购买：直接生成结算数据跳转确认订单页
    const orderData = [
      {
        goodsId: goods.value.id,
        skuId: sku.id,
        shopId: goods.value.shopId,
        shopName: shopName.value,
        title: goods.value.title,
        cover: goods.value.cover,
        specText,
        price: sku.price,
        quantity
      }
    ]
    uni.setStorageSync('direct_buy', orderData)
    uni.navigateTo({ url: '/pages/order/confirm' })
  }
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/cart' })
}
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg-white;
}
.detail-scroll {
  padding-bottom: 120rpx;
  box-sizing: border-box;
}
.banner {
  height: 750rpx;
  background: $bg-gray;
}
.banner-img {
  width: 100%;
  height: 750rpx;
}
.price-block {
  display: flex;
  align-items: baseline;
  padding: 28rpx 32rpx 8rpx;
  background: linear-gradient(180deg, $primary-light 0%, rgba(255, 241, 240, 0) 100%);
  margin: 0 -0rpx;
}
.price-symbol {
  font-size: 32rpx;
  color: $primary-color;
  font-weight: 600;
}
.price-num {
  font-size: 56rpx;
  font-weight: 800;
  color: $primary-color;
  letter-spacing: -1rpx;
  font-variant-numeric: tabular-nums;
}
.sales {
  margin-left: auto;
  font-size: 24rpx;
  color: $text-tertiary;
}
.title-block {
  padding: 8rpx 32rpx 20rpx;
}
.goods-title {
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.5;
  letter-spacing: 0.5rpx;
}
.shop-block {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;
  margin: 0 24rpx;
  background: $bg-gray;
  border-radius: $radius-md;
}
.shop-name {
  font-size: 26rpx;
  font-weight: 600;
}
.shop-enter {
  font-size: 24rpx;
  color: $text-secondary;
}
.spec-row {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  margin: 20rpx 24rpx 0;
  border-bottom: 1rpx solid $divider-color;
}
.spec-label {
  color: $text-secondary;
  font-size: 26rpx;
  margin-right: 24rpx;
}
.spec-value {
  flex: 1;
  font-size: 26rpx;
  font-weight: 500;
}
.arrow {
  color: #d1d1d6;
  font-size: 36rpx;
}
.desc-block {
  padding: 32rpx;
}
.desc-title {
  font-size: 28rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 20rpx;
  letter-spacing: 0.5rpx;
  position: relative;
  padding-left: 20rpx;
}
.desc-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6rpx;
  bottom: 6rpx;
  width: 6rpx;
  border-radius: 3rpx;
  background: $primary-gradient;
}
.desc-text {
  font-size: 26rpx;
  color: $text-secondary;
  line-height: 1.8;
}
.bottom-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  border-top: 1rpx solid $border-color;
  box-shadow: 0 -4rpx 20rpx rgba(29, 29, 36, 0.05);
}
.bar-icon {
  position: relative;
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.icon {
  font-size: 44rpx;
}
.badge {
  position: absolute;
  top: 4rpx;
  right: 4rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  background: $primary-gradient;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  padding: 0 6rpx;
  box-shadow: 0 4rpx 10rpx rgba(255, 77, 79, 0.35);
}
.bar-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: $radius-pill;
  font-size: 28rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
}
.cart-btn {
  background: linear-gradient(135deg, #ffd666, #fadb14);
  color: #7a4d00;
}
.buy-btn {
  background: $primary-gradient;
  color: #fff;
  box-shadow: $shadow-primary;
}
</style>
