<template>
  <scroll-view
    scroll-y
    class="waterfall-scroll"
    :style="{ height: scrollHeight }"
    @scroll="onScroll"
    @scrolltolower="emit('loadmore')"
    lower-threshold="300"
  >
    <view class="waterfall-body" :style="{ height: totalHeight + 'px', position: 'relative' }">
      <!-- 仅渲染可视区 + 上下预加载区（约15个节点） -->
      <view
        v-for="item in visibleItems"
        :key="item.data.id"
        :id="'wf-' + item.data.id"
        class="wf-item"
        :style="{
          position: 'absolute',
          top: item.top + 'px',
          left: item.left + 'px',
          width: colWidth + 'px'
        }"
      >
        <slot name="item" :item="item.data" />
      </view>
    </view>
  </scroll-view>
</template>

<script setup>
/**
 * 双列瀑布流虚拟列表（需求 4.1）
 * - DOM 节点控制：只渲染可视区 + 上下预加载区，其余节点从 DOM 树移除
 * - 变高测量：渲染后用 createSelectorQuery 实测真实高度并回填位置表
 * - 快速滑动时通过节流降低重排频率（图片暂停加载由 lazy-image 组件配合 scrollState 处理）
 */
import { computed, onMounted, ref, watch, nextTick } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  scrollHeight: { type: String, default: '100vh' },
  estimatedHeight: { type: Number, default: 260 }, // 预估高度（px）
  buffer: { type: Number, default: 4 } // 上下预加载项数
})
const emit = defineEmits(['loadmore', 'scroll-state'])

const colWidth = ref(180)
const positions = ref([]) // [{ data, top, left, height }]
const scrollTop = ref(0)
const viewportHeight = ref(600)
const totalHeight = computed(() => {
  if (!positions.value.length) return 0
  const last = positions.value[positions.value.length - 1]
  return last.top + last.height
})

/** 可视区 + 预加载区内的项 */
const visibleItems = computed(() => {
  const viewTop = scrollTop.value
  const viewBottom = viewTop + viewportHeight.value
  const buf = props.buffer * props.estimatedHeight
  return positions.value.filter(
    (p) => p.top + p.height > viewTop - buf && p.top < viewBottom + buf
  )
})

/** 依据当前高度表重新计算双列布局位置 */
function layout() {
  const colH = [0, 0]
  const list = []
  props.items.forEach((data) => {
    const prev = positions.value.find((p) => p.data.id === data.id)
    const height = prev ? prev.height : props.estimatedHeight
    const col = colH[0] <= colH[1] ? 0 : 1
    list.push({ data, top: colH[col], left: col * (colWidth.value + 12), height })
    colH[col] += height + 12
  })
  positions.value = list
  measure()
}

/** 实测渲染出的真实高度并回填（渐进增强：仅测可视区） */
function measure() {
  nextTick(() => {
    const query = uni.createSelectorQuery()
    const targets = visibleItems.value
    if (!targets.length) return
    targets.forEach((p) => {
      query.select('#wf-' + p.data.id).boundingClientRect((rect) => {
        if (rect && rect.height > 0 && Math.abs(rect.height - p.height) > 2) {
          p.height = rect.height
          reflowFrom(p)
        }
      })
    })
    query.exec()
  })
}

/** 某项高度变化后，重算其后所有项的位置 */
function reflowFrom(changed) {
  const idx = positions.value.indexOf(changed)
  if (idx < 0) return
  // 重新双列分配（保持已测高度）
  const colH = [0, 0]
  positions.value.forEach((p, i) => {
    const col = colH[0] <= colH[1] ? 0 : 1
    p.top = colH[col]
    p.left = col * (colWidth.value + 12)
    colH[col] += p.height + 12
  })
}

let lastScrollTop = 0
let scrollTimer = null
function onScroll(e) {
  scrollTop.value = e.detail.scrollTop
  // 滚动状态广播：快速滑动时 lazy-image 暂停图片请求（内存回收）
  const speed = Math.abs(e.detail.scrollTop - lastScrollTop)
  emit('scroll-state', speed > 40 ? 'fast' : 'idle')
  lastScrollTop = e.detail.scrollTop
  clearTimeout(scrollTimer)
  scrollTimer = setTimeout(() => emit('scroll-state', 'idle'), 150)
}

onMounted(() => {
  const sys = uni.getSystemInfoSync()
  const usable = sys.windowWidth - 24 // 左右边距
  colWidth.value = Math.floor((usable - 12) / 2)
  viewportHeight.value = parseInt(props.scrollHeight, 10) || sys.windowHeight
  layout()
})

watch(() => props.items.length, layout)
</script>

<style lang="scss" scoped>
.waterfall-scroll {
  width: 100%;
}
.waterfall-body {
  padding: 0 12rpx;
}
.wf-item {
  /* 位置由 style 控制 */
}
</style>
