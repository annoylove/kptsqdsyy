<template>
  <view v-if="visible" class="sku-mask" @tap="close">
    <view class="sku-panel" @tap.stop>
      <!-- 头部：图 + 价格 + 库存 -->
      <view class="sku-header">
        <image class="sku-img" :src="currentSku ? currentSku.cover : goods.cover" mode="aspectFill" />
        <view class="sku-info">
          <view class="price-row">
            <text class="price-symbol">¥</text>
            <text class="price-num">{{ displayPrice }}</text>
          </view>
          <text class="stock-tip">
            {{ currentSku ? `剩余库存 ${currentSku.stock} 件` : '请选择完整规格' }}
          </text>
          <text class="selected-tip" v-if="selectedText">已选：{{ selectedText }}</text>
        </view>
        <text class="close-btn" @tap="close">✕</text>
      </view>

      <!-- 规格列表 -->
      <scroll-view scroll-y class="spec-scroll">
        <view class="spec-group" v-for="spec in goods.specs" :key="spec.name">
          <text class="spec-name">{{ spec.name }}</text>
          <view class="spec-values">
            <view
              v-for="val in spec.values"
              :key="val"
              class="spec-value"
              :class="{
                active: selected[spec.name] === val,
                disabled: !isValueAvailable(spec.name, val)
              }"
              @tap="selectValue(spec.name, val)"
            >
              {{ val }}
            </view>
          </view>
        </view>

        <!-- 数量步进器 -->
        <view class="qty-row">
          <text class="qty-label">数量</text>
          <view class="stepper">
            <text class="step-btn" :class="{ dis: quantity <= 1 }" @tap="changeQty(-1)">−</text>
            <input
              class="step-input"
              type="number"
              :value="String(quantity)"
              @blur="onQtyInput"
            />
            <text class="step-btn" :class="{ dis: quantity >= maxQty }" @tap="changeQty(1)">＋</text>
          </view>
        </view>
      </scroll-view>

      <view class="sku-footer safe-area-bottom">
        <view
          class="confirm-btn press-effect"
          :class="{ dis: !currentSku || currentSku.stock <= 0 }"
          @tap="confirm"
        >
          {{ mode === 'cart' ? '加入购物车' : '立即购买' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
/**
 * SKU 选择面板（需求 2.2）
 * - 规格联动：选中"颜色:红色"后，"尺寸"中无库存的值置灰不可点
 * - 动态价格与库存：选中有效 SKU 后实时更新
 * - 数量步进器：默认 1，达到该 SKU 最大库存时 + 置灰
 */
import { computed, reactive, ref, watch } from 'vue'

const props = defineProps({
  visible: Boolean,
  goods: { type: Object, default: () => ({ specs: [], skus: [] }) },
  mode: { type: String, default: 'cart' } // cart | buy
})
const emit = defineEmits(['update:visible', 'confirm'])

const selected = reactive({})
const quantity = ref(1)

watch(
  () => props.visible,
  (v) => {
    if (v) {
      Object.keys(selected).forEach((k) => delete selected[k])
      quantity.value = 1
    }
  }
)

/** 查找与给定规格值组合匹配且库存>0 的 SKU */
function findSku(partial) {
  return props.goods.skus.find(
    (s) =>
      s.stock > 0 &&
      Object.keys(partial).every((k) => !partial[k] || s.specValues[k] === partial[k])
  )
}

/**
 * 判断某规格值在当前已选条件下是否可用（有库存组合）
 * 核心联动算法：把该值代入，检查是否存在库存>0 的 SKU
 */
function isValueAvailable(specName, value) {
  const partial = { ...selected, [specName]: value }
  return !!findSku(partial)
}

/** 点击规格值：已选中则取消，未选中则切换；置灰项不可点 */
function selectValue(specName, value) {
  if (!isValueAvailable(specName, value)) {
    uni.showToast({ title: '该规格暂时缺货', icon: 'none' })
    return
  }
  selected[specName] = selected[specName] === value ? '' : value
}

/** 当前完整匹配的 SKU */
const currentSku = computed(() => {
  const full = props.goods.specs.every((s) => selected[s.name])
  if (!full) return null
  return (
    props.goods.skus.find((s) =>
      props.goods.specs.every((spec) => s.specValues[spec.name] === selected[spec.name])
    ) || null
  )
})

const displayPrice = computed(() => {
  if (currentSku.value) return currentSku.value.price.toFixed(2)
  // 未选完整规格时展示最低价
  const prices = props.goods.skus.map((s) => s.price)
  return prices.length ? Math.min(...prices).toFixed(2) : '0.00'
})

const selectedText = computed(() => {
  const parts = props.goods.specs
    .map((s) => selected[s.name])
    .filter(Boolean)
  return parts.join(' ')
})

const maxQty = computed(() => (currentSku.value ? currentSku.value.stock : 1))

function changeQty(delta) {
  const next = quantity.value + delta
  if (next < 1) return
  if (next > maxQty.value) {
    uni.showToast({ title: `已达到最大库存 ${maxQty.value}`, icon: 'none' })
    return
  }
  quantity.value = next
}

function onQtyInput(e) {
  let v = parseInt(e.detail.value, 10)
  if (isNaN(v) || v < 1) v = 1
  if (v > maxQty.value) {
    uni.showToast({ title: `库存仅剩 ${maxQty.value} 件`, icon: 'none' })
    v = maxQty.value
  }
  quantity.value = v
}

function close() {
  emit('update:visible', false)
}

function confirm() {
  if (!currentSku.value) {
    uni.showToast({ title: '请选择完整规格', icon: 'none' })
    return
  }
  if (currentSku.value.stock <= 0) return
  emit('confirm', {
    sku: currentSku.value,
    quantity: quantity.value,
    specText: selectedText.value
  })
  close()
}
</script>

<style lang="scss" scoped>
.sku-mask {
  position: fixed;
  inset: 0;
  background: rgba(29, 29, 36, 0.55);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  animation: mask-in 0.25s ease both;
}
@keyframes mask-in {
  from { opacity: 0; }
  to { opacity: 1; }
}
.sku-panel {
  width: 100%;
  background: $bg-white;
  border-radius: $radius-xl $radius-xl 0 0;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  animation: panel-in 0.3s cubic-bezier(0.4, 0, 0.2, 1) both;
}
@keyframes panel-in {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.sku-header {
  display: flex;
  padding: 32rpx 24rpx 24rpx;
  position: relative;
}
.sku-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-md;
  background: $bg-gray;
  margin-top: -60rpx;
  box-shadow: $shadow-float;
  border: 4rpx solid $bg-white;
}
.sku-info {
  flex: 1;
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.price-row {
  color: $primary-color;
}
.price-symbol {
  font-size: 28rpx;
  font-weight: 600;
}
.price-num {
  font-size: 48rpx;
  font-weight: 800;
  letter-spacing: -1rpx;
  font-variant-numeric: tabular-nums;
}
.stock-tip {
  font-size: 24rpx;
  color: $text-tertiary;
  margin-top: 10rpx;
}
.selected-tip {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: 6rpx;
  background: $bg-gray;
  display: inline-block;
  padding: 6rpx 18rpx;
  border-radius: $radius-pill;
  align-self: flex-start;
  max-width: 100%;
}
.close-btn {
  position: absolute;
  right: 24rpx;
  top: 24rpx;
  width: 56rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  font-size: 30rpx;
  color: $text-tertiary;
  background: $bg-gray;
  border-radius: 50%;
}
.spec-scroll {
  max-height: 40vh;
  padding: 0 32rpx;
}
.spec-group {
  padding: 28rpx 0 12rpx;
}
.spec-name {
  font-size: 28rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 20rpx;
  letter-spacing: 0.5rpx;
}
.spec-values {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}
.spec-value {
  padding: 14rpx 32rpx;
  background: $bg-gray;
  border-radius: $radius-pill;
  font-size: 26rpx;
  border: 2rpx solid transparent;
  color: $text-color;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.spec-value.active {
  background: $primary-light;
  color: $primary-color;
  border-color: $primary-color;
  font-weight: 600;
  box-shadow: 0 4rpx 10rpx rgba(255, 77, 79, 0.12);
}
.spec-value.disabled {
  color: #d1d1d6;
  background: #fafafa;
  text-decoration: line-through;
}
.qty-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 36rpx 0;
}
.qty-label {
  font-size: 28rpx;
  font-weight: 700;
}
.stepper {
  display: flex;
  align-items: center;
  border: 1rpx solid #e5e5ea;
  border-radius: $radius-pill;
  overflow: hidden;
}
.step-btn {
  width: 72rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  font-size: 36rpx;
  background: $bg-gray;
  color: $text-secondary;
}
.step-btn.dis {
  color: #d1d1d6;
}
.step-input {
  width: 90rpx;
  height: 64rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
}
.sku-footer {
  padding: 20rpx 32rpx;
  border-top: 1rpx solid $divider-color;
}
.confirm-btn {
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  background: $primary-gradient;
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  border-radius: $radius-pill;
  box-shadow: $shadow-primary;
}
.confirm-btn.dis {
  background: $bg-gray;
  color: $text-placeholder;
  box-shadow: none;
}
</style>
