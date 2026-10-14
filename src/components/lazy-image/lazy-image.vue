<template>
  <view class="lazy-image" :style="{ height: height }">
    <!-- 低像素模糊占位（渐进式加载第一层） -->
    <image
      v-if="!loaded && !error"
      class="img blur-img"
      :src="thumbSrc"
      mode="aspectFill"
    />
    <!-- 骨架屏 -->
    <view v-if="!loaded && !error && !thumbSrc" class="skeleton"></view>
    <!-- 原图淡入（第二层） -->
    <image
      v-if="shouldLoad && !error"
      class="img real-img"
      :class="{ 'fade-in': loaded }"
      :src="src"
      mode="aspectFill"
      @load="onLoad"
      @error="onError"
    />
    <view v-if="error" class="error-box" @tap="retry">
      <text>加载失败，点击重试</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 渐进式加载图片（需求 4.1）
 * - 先展示低像素模糊图/骨架屏，原图下载完成后淡入切换
 * - 快速滑动（scrollState === 'fast'）时暂停原图请求，优先加载停留屏幕的图片
 */
import { computed, ref, watch } from 'vue'

const props = defineProps({
  src: { type: String, required: true },
  thumb: { type: String, default: '' }, // 低像素缩略图
  height: { type: String, default: '200px' },
  scrollState: { type: String, default: 'idle' } // idle | fast
})

const loaded = ref(false)
const error = ref(false)
const retryKey = ref(0)

/** 快速滑动时暂停未加载图片的请求 */
const shouldLoad = computed(() => {
  if (loaded.value) return true
  return props.scrollState !== 'fast'
})

const thumbSrc = computed(() => {
  if (props.thumb) return props.thumb
  // 用 picsum 的小尺寸作为模糊占位
  if (props.src.includes('picsum.photos')) {
    return props.src.replace(/\/\d+\/\d+$/, '/40/40')
  }
  return ''
})

function onLoad() {
  loaded.value = true
}
function onError() {
  error.value = true
}
function retry() {
  error.value = false
  loaded.value = false
  retryKey.value++
}

// 虚拟列表复用节点时重置状态
watch(
  () => props.src,
  () => {
    loaded.value = false
    error.value = false
  }
)
</script>

<style lang="scss" scoped>
.lazy-image {
  position: relative;
  overflow: hidden;
  width: 100%;
}
.img {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
}
.blur-img {
  filter: blur(12px);
  transform: scale(1.05);
}
.real-img {
  opacity: 0;
}
.real-img.fade-in {
  opacity: 1;
  transition: opacity 0.4s ease-in;
}
.skeleton {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, #f2f3f5 25%, #e8e9ed 50%, #f2f3f5 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
.error-box {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f7f7;
  font-size: 22rpx;
  color: #999;
}
</style>
