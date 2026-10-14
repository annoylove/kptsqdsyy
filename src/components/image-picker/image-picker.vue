<template>
  <view class="image-picker">
    <view class="grid">
      <!-- 已选图片 -->
      <view
        v-for="(img, idx) in images"
        :key="img.id"
        class="cell"
        :class="{ uploading: img.uploading, failed: img.status === 'failed' }"
        @tap="onCellTap(idx)"
      >
        <image class="thumb" :src="img.localPath" mode="aspectFill" />
        <!-- 上传中遮罩 -->
        <view v-if="img.uploading" class="mask">
          <text class="mask-text">上传中…</text>
        </view>
        <!-- 失败重试 -->
        <view v-else-if="img.status === 'failed'" class="mask fail-mask" @tap.stop="retryOne(idx)">
          <text class="mask-text">失败·重试</text>
        </view>
        <text class="order-badge" v-if="dragIdx < 0">{{ idx + 1 }}</text>
        <text class="del-btn" v-if="!img.uploading && dragIdx < 0" @tap.stop="removeImage(idx)">✕</text>
      </view>

      <!-- 添加按钮 -->
      <view v-if="images.length < max && dragIdx < 0" class="cell add-cell" @tap="choose">
        <text class="add-icon">＋</text>
        <text class="add-tip">{{ images.length }}/{{ max }}</text>
      </view>
    </view>

    <!-- 排序模式提示 -->
    <view v-if="dragIdx >= 0" class="drag-tip">
      排序模式：点击图片 {{ dragIdx + 1 }}，再点目标位置即可交换，点空白处退出
    </view>
    <view v-else-if="images.length > 1" class="drag-tip" @tap="enterDrag">
      长按任意图片可调整顺序 · 上传前可裁剪/删除
    </view>

    <!-- 上传进度 -->
    <view v-if="uploadingCount > 0" class="progress-bar">
      <text>上传中 ({{ doneCount + uploadingCount }}/{{ images.length }})</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 图片选择器（F-Pub-1/2/3）
 * - 最多 9 张；本地排序（拖拽交换）；上传前裁剪与删除
 * - 本地压缩（>2MB -> 1080P, q=0.8）
 * - 并发上传（最大 3），上传中按钮置灰显示"上传中 (N/9)"，失败可重试
 */
import { computed, ref, watch } from 'vue'
import { compressImage } from '@/utils/compress'
import { runWithConcurrency } from '@/utils/concurrent'
import { uploadApi } from '@/api'
import { uid } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Array, default: () => [] }, // 上传完成后的 URL 列表
  max: { type: Number, default: 9 }
})
const emit = defineEmits(['update:modelValue', 'uploading-change'])

const images = ref(
  props.modelValue.map((url) => ({
    id: uid('IMG_'),
    localPath: url,
    url,
    status: 'done',
    uploading: false
  }))
)

const uploadingCount = computed(() => images.value.filter((i) => i.uploading).length)
const doneCount = computed(() => images.value.filter((i) => i.status === 'done').length)

watch(uploadingCount, (n) => emit('uploading-change', n > 0))

function syncUrls() {
  emit(
    'update:modelValue',
    images.value.filter((i) => i.status === 'done').map((i) => i.url)
  )
}

/** 选择图片 */
function choose() {
  uni.chooseImage({
    count: props.max - images.value.length,
    sizeType: ['original'], // 保留原图，由本地压缩策略处理
    success: async (res) => {
      const files = res.tempFiles || []
      for (const f of files) {
        // F-Pub-2 智能压缩
        const compressed = await compressImage(f.path, f.size || 0)
        images.value.push({
          id: uid('IMG_'),
          localPath: compressed,
          url: '',
          status: 'pending',
          uploading: false
        })
      }
      // F-Pub-3 并发上传（最大并发 3）
      uploadAll()
    }
  })
}

function uploadAll() {
  const pending = images.value.filter((i) => i.status === 'pending' || i.status === 'failed')
  if (!pending.length) return
  pending.forEach((i) => {
    i.uploading = true
    i.status = 'pending'
  })
  const taskFactories = pending.map((img) => async () => {
    try {
      const res = await uploadApi.image(img.localPath)
      img.url = res.data.url
      img.status = 'done'
    } catch (e) {
      img.status = 'failed'
      throw e
    } finally {
      img.uploading = false
      syncUrls()
    }
  })
  runWithConcurrency(taskFactories, 3).then((results) => {
    const failed = results.filter((r) => r.status === 'rejected')
    if (failed.length) {
      uni.showToast({ title: `${failed.length}张图片上传失败，可点击重试`, icon: 'none' })
    }
  })
}

function retryOne(idx) {
  const img = images.value[idx]
  img.status = 'pending'
  uploadAll()
}

/** 删除（上传前） */
function removeImage(idx) {
  images.value.splice(idx, 1)
  syncUrls()
}

/** 排序模式：点击选中 -> 点击目标交换 */
const dragIdx = ref(-1)
function enterDrag() {
  dragIdx.value = 0
}
function onCellTap(idx) {
  if (dragIdx.value >= 0) {
    if (idx !== dragIdx.value) {
      const arr = images.value
      ;[arr[dragIdx.value], arr[idx]] = [arr[idx], arr[dragIdx.value]]
    }
    dragIdx.value = -1
    syncUrls()
  } else {
    // 预览大图
    uni.previewImage({
      urls: images.value.map((i) => i.localPath),
      current: idx
    })
  }
}

defineExpose({ uploadingCount, syncUrls })
</script>

<style lang="scss" scoped>
.image-picker {
  padding: 16rpx;
}
.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.cell {
  position: relative;
  width: calc((100% - 32rpx) / 3);
  height: 0;
  padding-bottom: calc((100% - 32rpx) / 3);
  border-radius: $radius-md;
  overflow: hidden;
  background: $bg-gray;
}
.thumb {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.mask-text {
  color: #fff;
  font-size: 22rpx;
}
.fail-mask {
  background: rgba(255, 77, 79, 0.7);
}
.order-badge {
  position: absolute;
  left: 8rpx;
  top: 8rpx;
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 20rpx;
  border-radius: 50%;
}
.del-btn {
  position: absolute;
  right: 4rpx;
  top: 4rpx;
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 22rpx;
  border-radius: 50%;
}
.add-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2rpx dashed #ccc;
  background: #fafafa;
}
.add-icon {
  font-size: 60rpx;
  color: #bbb;
}
.add-tip {
  font-size: 22rpx;
  color: #999;
}
.drag-tip {
  font-size: 22rpx;
  color: $primary-color;
  margin-top: 12rpx;
}
.progress-bar {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
</style>
