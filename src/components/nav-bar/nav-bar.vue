<template>
  <!-- fixed 导航栏本体 -->
  <view class="nav-bar" :class="{ glass, hairline: !glass && backgroundColor !== 'transparent' }" :style="barStyle">
    <!-- 状态栏占位：高度 = statusBarHeight -->
    <view :style="{ height: statusBarHeight + 'px' }"></view>
    <!-- 导航内容区：固定 44px -->
    <view class="nav-content" :style="{ height: '44px' }">
      <view class="nav-left" @tap="onBack" v-if="showBack">
        <view class="back-btn" :class="{ light: theme === 'white' }">
          <text class="back-icon" :style="{ color: textColor }">‹</text>
        </view>
      </view>
      <text class="nav-title" :style="{ color: textColor }">{{ title }}</text>
      <slot name="right" />
    </view>
  </view>
  <!-- 流内占位（必须在 fixed 容器外，否则会把导航栏撑高并遮挡页面内容） -->
  <view v-if="fixed" :style="{ height: totalHeight + 'px' }" class="placeholder"></view>
</template>

<script setup>
/**
 * 自定义导航栏（需求 3.1）
 * - 微信小程序：uni.getSystemInfoSync() 获取 statusBarHeight，高度 = statusBarHeight + 44px
 * - H5：隐藏原生导航栏，前端自定义；底部安全区由页面 .safe-area-bottom 处理
 * - APP：沉浸式状态栏（背景色延伸至状态栏区域）
 * - glass 模式：玻璃拟态（半透明 + 背景模糊），滚动内容透出更高级
 */
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: true },
  fixed: { type: Boolean, default: true },
  backgroundColor: { type: String, default: '#ffffff' },
  glass: { type: Boolean, default: false },
  theme: { type: String, default: 'black' } // black | white（APP 沉浸式根据背景切换文字颜色）
})

const systemInfo = uni.getSystemInfoSync()
const statusBarHeight = systemInfo.statusBarHeight || 0
const totalHeight = computed(() => statusBarHeight + 44)
const textColor = computed(() => (props.theme === 'white' ? '#ffffff' : '#1d1d24'))
const barStyle = computed(() => (props.glass ? {} : { backgroundColor: props.backgroundColor }))

function onBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/index/index' })
  }
}
</script>

<style lang="scss" scoped>
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
}
.nav-bar.glass {
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
}
.nav-bar.hairline {
  border-bottom: 1rpx solid $border-color;
}
.nav-content {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 0 12rpx;
}
.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  max-width: 60%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.nav-left {
  position: absolute;
  left: 16rpx;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
}
.back-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(29, 29, 36, 0.05);
  transition: all 0.2s ease;
}
.back-btn:active {
  transform: scale(0.88);
  background: rgba(29, 29, 36, 0.12);
}
.back-btn.light {
  background: rgba(255, 255, 255, 0.22);
}
.back-icon {
  font-size: 52rpx;
  line-height: 1;
  margin-top: -4rpx;
}
</style>
