<template>
  <view class="page">
    <nav-bar title="商城" :show-back="false" />

    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索精选好物"
          placeholder-class="search-ph"
          confirm-type="search"
          @confirm="search"
        />
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="goods-grid">
      <template v-if="loading">
        <view class="sk-card" v-for="i in 6" :key="'sk' + i">
          <view class="skeleton sk-img"></view>
          <view class="sk-info">
            <view class="skeleton sk-line"></view>
            <view class="skeleton sk-line sk-short"></view>
          </view>
        </view>
      </template>
      <view class="goods-card press-effect" v-for="g in goodsList" :key="g.id" @tap="goDetail(g.id)">
        <lazy-image :src="g.cover" height="160px" />
        <view class="goods-info">
          <text class="goods-title">{{ g.title }}</text>
          <view class="goods-foot">
            <text class="goods-price">¥{{ g.price }}</text>
            <text class="goods-sales">已售{{ g.sales }}</text>
          </view>
        </view>
      </view>
      <view v-if="!loading && !goodsList.length" class="empty">
        <text class="empty-icon">🔍</text>
        <text class="empty-text">没有找到相关商品</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { goodsApi } from '@/api'

const systemInfo = uni.getSystemInfoSync()
const navHeight = (systemInfo.statusBarHeight || 0) + 44
const keyword = ref('')
const goodsList = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const res = await goodsApi.list({ keyword: keyword.value })
    goodsList.value = res.data.list
  } finally {
    loading.value = false
  }
}

function search() {
  load()
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${id}` })
}

onLoad(() => load())
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-color;
}
.search-bar {
  padding: 20rpx 24rpx;
  background: $bg-white;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
  height: 76rpx;
  background: $bg-gray;
  border-radius: $radius-pill;
  padding: 0 32rpx;
  border: 1rpx solid $border-color;
}
.search-icon {
  font-size: 26rpx;
  opacity: 0.5;
}
.search-input {
  flex: 1;
  font-size: 26rpx;
}
.search-ph {
  color: $text-placeholder;
}
.goods-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 16rpx 24rpx;
  gap: 16rpx;
}
.goods-card {
  width: calc(50% - 8rpx);
  background: $bg-white;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.goods-card:active {
  transform: scale(0.97);
  box-shadow: $shadow-float;
}
.sk-card {
  width: calc(50% - 8rpx);
  background: $bg-white;
  border-radius: $radius-lg;
  overflow: hidden;
  border: 1rpx solid $border-color;
}
.sk-img {
  height: 320rpx;
  border-radius: 0;
}
.sk-info {
  padding: 20rpx;
}
.sk-line {
  height: 26rpx;
  margin-bottom: 16rpx;
}
.sk-short {
  width: 50%;
  margin-bottom: 0;
}
.goods-info {
  padding: 20rpx;
}
.goods-title {
  font-size: 26rpx;
  line-height: 1.5;
  height: 78rpx;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-weight: 500;
}
.goods-foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 16rpx;
}
.goods-price {
  color: $price-color;
  font-size: 34rpx;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.goods-sales {
  font-size: 22rpx;
  color: $text-tertiary;
}
.empty {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
  color: $text-tertiary;
}
.empty-icon {
  font-size: 100rpx;
  opacity: 0.4;
  filter: grayscale(0.4);
  margin-bottom: 24rpx;
}
.empty-text {
  font-size: 26rpx;
  letter-spacing: 1rpx;
}
</style>
