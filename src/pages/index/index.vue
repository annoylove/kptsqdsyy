<template>
  <view class="page">
    <nav-bar title="社区" :show-back="false" />

    <view class="feed">
      <!-- 首屏骨架屏 -->
      <view v-if="loading && !posts.length" class="sk-grid">
        <view class="sk-col" v-for="c in 2" :key="c">
          <view class="sk-card" v-for="i in 3" :key="i">
            <view class="skeleton" :style="{ height: (140 + ((i + c) % 3) * 60) + 'px' }"></view>
            <view class="sk-body">
              <view class="skeleton sk-line"></view>
              <view class="skeleton sk-line sk-short"></view>
              <view class="sk-foot">
                <view class="skeleton sk-avatar"></view>
                <view class="skeleton sk-line sk-tiny"></view>
              </view>
            </view>
          </view>
        </view>
      </view>
      <waterfall-virtual
        v-else
        :items="posts"
        :scroll-height="scrollHeight"
        @loadmore="loadMore"
        @scroll-state="onScrollState"
      >
        <template #item="{ item }">
          <view class="post-card press-effect" @tap="goDetail(item.id)">
            <lazy-image
              :src="item.images[0] || placeholder"
              :height="coverHeight(item) + 'px'"
              :scroll-state="scrollState"
            />
            <view class="post-body">
              <text class="post-title">{{ item.title }}</text>
              <view class="post-foot">
                <view class="author">
                  <image class="avatar" :src="item.author.avatar" mode="aspectFill" />
                  <text class="name">{{ item.author.nickname }}</text>
                </view>
                <view class="like press-effect" @tap.stop="toggleLike(item)">
                  <text :class="{ liked: item.liked }">♥</text>
                  <text>{{ item.likes }}</text>
                </view>
              </view>
            </view>
          </view>
        </template>
      </waterfall-virtual>
    </view>

    <!-- 发布悬浮按钮 -->
    <view class="fab press-effect" @tap="goPublish">
      <text>✎</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { postApi } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const posts = ref([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const scrollState = ref('idle')
const placeholder = 'https://picsum.photos/seed/ph/400/400'

const systemInfo = uni.getSystemInfoSync()
const navHeight = (systemInfo.statusBarHeight || 0) + 44
const scrollHeight = computed(() => `${systemInfo.windowHeight - navHeight}px`)

async function fetchList(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 1
    hasMore.value = true
    posts.value = []
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const res = await postApi.list({ page: page.value, size: 10 })
    posts.value = posts.value.concat(res.data.list)
    hasMore.value = res.data.hasMore
    page.value++
  } finally {
    loading.value = false
  }
}

function loadMore() {
  fetchList()
}

function onScrollState(state) {
  scrollState.value = state
}

/** 依据图片数量估算封面高度，制造瀑布流错落效果 */
function coverHeight(item) {
  const base = 160
  const n = item.images.length
  return base + (n % 3) * 60
}

async function toggleLike(item) {
  if (!userStore.requireLogin()) return
  item.liked = !item.liked
  const res = await postApi.like(item.id, item.liked)
  item.likes = res.data.likes
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/post/detail?id=${id}` })
}
function goPublish() {
  if (!userStore.requireLogin()) return
  uni.navigateTo({ url: '/pages/publish/publish' })
}

onShow(() => fetchList(true))
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: $bg-color;
}
.feed {
  padding: 0;
}
.sk-grid {
  display: flex;
  gap: 12px;
  padding: 12px 12px 0;
}
.sk-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sk-card {
  background: $bg-white;
  border-radius: $radius-lg;
  overflow: hidden;
  border: 1rpx solid $border-color;
}
.sk-body {
  padding: 20rpx;
}
.sk-line {
  height: 28rpx;
  margin-bottom: 14rpx;
}
.sk-short {
  width: 60%;
}
.sk-tiny {
  width: 40%;
  height: 22rpx;
  margin-bottom: 0;
}
.sk-foot {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 20rpx;
}
.sk-avatar {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
}
.post-card {
  background: $bg-white;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.post-card:active {
  transform: scale(0.98);
  box-shadow: $shadow-float;
}
.post-body {
  padding: 20rpx;
}
.post-title {
  font-size: 28rpx;
  font-weight: 600;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  letter-spacing: 0.5rpx;
}
.post-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}
.author {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.avatar {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  border: 2rpx solid $border-color;
}
.name {
  font-size: 24rpx;
  color: $text-secondary;
  font-weight: 500;
}
.like {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: $text-tertiary;
  transition: all 0.2s ease;
}
.like .liked {
  color: $primary-color;
  transform: scale(1.1);
}
.fab {
  position: fixed;
  right: 40rpx;
  bottom: 140rpx;
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: $primary-gradient;
  color: #fff;
  font-size: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-primary;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.fab:active {
  transform: scale(0.92);
  box-shadow: 0 4rpx 12rpx rgba(255, 77, 79, 0.3);
}
</style>
