<template>
  <view class="page">
    <scroll-view scroll-y class="form-scroll">
      <input
        class="title-input"
        v-model="title"
        placeholder="添加标题"
        maxlength="50"
        @input="onInput"
      />
      <textarea
        class="content-input"
        v-model="content"
        placeholder="分享你的社区生活…"
        maxlength="2000"
        @input="onInput"
      />

      <!-- 图片选择器（最多9张/排序/压缩/并发上传） -->
      <image-picker v-model="images" @uploading-change="onUploadingChange" />

      <view class="draft-tip" v-if="draftSavedAt">
        草稿已自动保存 {{ draftSavedAt }}
      </view>
    </scroll-view>

    <view class="pub-bar safe-area-bottom">
      <view
        class="pub-btn press-effect"
        :class="{ dis: disabledPublish }"
        @tap="publish"
      >
        {{ uploading ? `上传中 (${imageCount}/9)` : publishing ? '发布中…' : '发布' }}
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * 社区发布页（F-Pub-1~4 + 4.2 防刷）
 * - 草稿箱自动保存：onChange 防抖 1000ms 写入 uni.setStorageSync('post_draft')
 * - 发布防抖：点击后按钮置灰 Loading，3 秒内禁止再次点击
 */
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { debounce } from '@/utils/debounce'
import { postApi } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const title = ref('')
const content = ref('')
const images = ref([])
const uploading = ref(false)
const imageCount = computed(() => images.value.length)
const publishing = ref(false)
const draftSavedAt = ref('')
const lastClickAt = ref(0)

// F-Pub-4：防抖 1000ms 自动保存草稿
const saveDraft = debounce(() => {
  uni.setStorageSync(
    'post_draft',
    JSON.stringify({ title: title.value, content: content.value, images: images.value, time: Date.now() })
  )
  const d = new Date()
  const p = (n) => (n < 10 ? '0' + n : n)
  draftSavedAt.value = `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}, 1000)

function onInput() {
  saveDraft()
}

function onUploadingChange(v) {
  uploading.value = v
}

// 发布防刷：上传中 / 发布中 / 3秒内重复点击 -> 禁用
const disabledPublish = computed(
  () => uploading.value || publishing.value
)

onLoad(() => {
  // 恢复草稿
  const raw = uni.getStorageSync('post_draft')
  if (raw) {
    try {
      const draft = JSON.parse(raw)
      title.value = draft.title || ''
      content.value = draft.content || ''
      images.value = draft.images || []
    } catch (e) {}
  }
})

async function publish() {
  if (disabledPublish.value) return
  // 3 秒防抖窗口
  const now = Date.now()
  if (now - lastClickAt.value < 3000) {
    uni.showToast({ title: '请勿重复点击', icon: 'none' })
    return
  }
  lastClickAt.value = now

  if (!title.value.trim() && !content.value.trim()) {
    uni.showToast({ title: '写点什么吧', icon: 'none' })
    return
  }
  if (!userStore.requireLogin()) return

  publishing.value = true
  try {
    await postApi.publish({
      title: title.value.trim() || '无标题动态',
      content: content.value.trim(),
      images: images.value
    })
    uni.removeStorageSync('post_draft')
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
    uni.showToast({ title: e.message || '发布失败', icon: 'none' })
  } finally {
    publishing.value = false
  }
}

onUnload(() => {
  // 离开页面立即保存一次草稿
  saveDraft()
})
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg-white;
}
.form-scroll {
  flex: 1;
  padding: 32rpx;
}
.title-input {
  font-size: 40rpx;
  font-weight: 700;
  letter-spacing: 0.5rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $divider-color;
}
.content-input {
  width: 100%;
  min-height: 260rpx;
  font-size: 29rpx;
  line-height: 1.8;
  padding: 28rpx 0;
  color: #3a3a42;
}
.draft-tip {
  display: inline-block;
  font-size: 22rpx;
  color: $success-color;
  background: rgba(52, 199, 89, 0.1);
  padding: 8rpx 24rpx;
  border-radius: $radius-pill;
  margin-top: 20rpx;
}
.pub-bar {
  padding: 20rpx 32rpx;
  background: $glass-bg;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  border-top: 1rpx solid $border-color;
  box-shadow: 0 -4rpx 20rpx rgba(29, 29, 36, 0.05);
}
.pub-btn {
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  background: $primary-gradient;
  color: #fff;
  border-radius: $radius-pill;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
  box-shadow: $shadow-primary;
}
.pub-btn.dis {
  background: $bg-gray;
  color: $text-placeholder;
  box-shadow: none;
}
</style>
