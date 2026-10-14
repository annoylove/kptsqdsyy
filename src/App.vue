<script setup>
import { onLaunch } from '@dcloudio/uni-app'
import { useOrderStore } from '@/store/order'

onLaunch(() => {
  // 应用启动时启动订单超时定时器（30分钟未支付自动取消 / 发货后15天自动确认）
  const orderStore = useOrderStore()
  orderStore.startTimers()
})
</script>

<style lang="scss">
/* ===== 全局基础 ===== */
page {
  background-color: $bg-color;
  font-size: $font-md;
  color: $text-color;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

view,
text {
  box-sizing: border-box;
}

/* ===== 按压反馈（统一交互反馈规范） ===== */
.press-effect {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1), transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.press-effect:active {
  opacity: 0.75;
  transform: scale(0.97);
}

/* ===== iOS 底部安全区适配（H5 / APP） ===== */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ===== 统一卡片规范（高级感：微边框 + 柔和投影 + 悬停微交互） ===== */
.uni-card {
  background: $bg-white;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
  overflow: hidden;
}

/* ===== 统一按钮规范（渐变 + 阴影 + 按压反馈） ===== */
.uni-btn {
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: $radius-pill;
  font-size: $font-md;
  font-weight: 600;
  letter-spacing: 0.5rpx;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.uni-btn-primary {
  background: $primary-gradient;
  color: #fff;
  box-shadow: $shadow-primary;
}
.uni-btn-primary:active {
  transform: scale(0.97);
  box-shadow: 0 4rpx 12rpx rgba(255, 77, 79, 0.25);
}
.uni-btn-disabled {
  background: $bg-gray !important;
  color: $text-placeholder !important;
  box-shadow: none !important;
}

/* ===== 统一空态（插画化图标 + 柔和渐变） ===== */
.uni-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
  color: $text-tertiary;
}
.uni-empty-icon {
  font-size: 120rpx;
  margin-bottom: 32rpx;
  opacity: 0.5;
  filter: grayscale(0.3);
}
.uni-empty-text {
  font-size: $font-md;
  letter-spacing: 1rpx;
}

/* ===== 骨架屏（渐进式加载视觉规范） ===== */
.skeleton {
  background: linear-gradient(90deg, #f2f3f5 25%, #e8e9ed 37%, #f2f3f5 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
  border-radius: $radius-sm;
}
@keyframes skeleton-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* ===== 价格统一样式（字重 + 字距） ===== */
.uni-price {
  color: $price-color;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.5rpx;
}

/* ===== 文本截断工具 ===== */
.ellipsis-1 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 淡入动画（页面内容进场） ===== */
.fade-in {
  animation: fade-in 0.4s cubic-bezier(0.4, 0, 0.2, 1) both;
}
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(16rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 玻璃拟态（导航栏/TabBar） ===== */
.glass {
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
}
</style>
