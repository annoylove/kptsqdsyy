/**
 * 购物车 Store —— 需求 1.2 核心计算逻辑
 * - 实时价格计算：单品总价 = 规格单价 * 数量（前端本地计算）
 * - 店铺级联选：全选店铺 -> 该店商品全选中；该店商品全选 -> 店铺勾选框自动勾选
 * - 失效商品处理：下架/库存归零/规格失效 -> 移入"失效商品"分组，不可勾选结算
 * - 库存边界校验：数量不能大于 SKU 实际库存；结算时后端二次拦截
 */
import { defineStore } from 'pinia'
import { goodsApi, stockApi } from '@/api'

export const useCartStore = defineStore('cart', {
  state: () => ({
    // items: [{ goodsId, skuId, shopId, shopName, title, cover, specText, price, quantity, stock, status }]
    items: uni.getStorageSync('cart_items') || [],
    // 选中状态（仅有效商品可被选中）
    selected: uni.getStorageSync('cart_selected') || {}
  }),

  getters: {
    /** 有效商品 */
    validItems: (state) => state.items.filter((i) => i.status === 'valid'),
    /** 失效商品（下架/无库存/规格失效） */
    invalidItems: (state) => state.items.filter((i) => i.status !== 'valid'),

    /** 按店铺分组（仅有效商品参与结算展示） */
    shopGroups: (state) => {
      const map = {}
      state.items
        .filter((i) => i.status === 'valid')
        .forEach((i) => {
          if (!map[i.shopId]) map[i.shopId] = { shopId: i.shopId, shopName: i.shopName, items: [] }
          map[i.shopId].items.push(i)
        })
      return Object.values(map)
    },

    /** 店铺是否全选（级联：该店所有有效商品都选中 -> 店铺勾选框勾选） */
    isShopAllSelected() {
      return (shopId) => {
        const items = this.validItems.filter((i) => i.shopId === shopId)
        if (!items.length) return false
        return items.every((i) => this.selected[i.skuId])
      }
    },

    /** 全选状态 */
    isAllSelected: (state) => {
      const valid = state.items.filter((i) => i.status === 'valid')
      if (!valid.length) return false
      return valid.every((i) => state.selected[i.skuId])
    },

    /** 已选商品列表 */
    selectedItems: (state) =>
      state.items.filter((i) => i.status === 'valid' && state.selected[i.skuId]),

    /** 已选总件数 */
    selectedCount: (state) =>
      state.items.reduce(
        (s, i) => (i.status === 'valid' && state.selected[i.skuId] ? s + i.quantity : s),
        0
      ),

    /** 结算总价（实时：单价 * 数量 累加） */
    totalPrice() {
      return this.selectedItems.reduce((s, i) => s + i.price * i.quantity, 0)
    },

    /** 购物车角标数量 */
    badgeCount: (state) => state.items.reduce((s, i) => s + i.quantity, 0)
  },

  actions: {
    _persist() {
      uni.setStorageSync('cart_items', this.items)
      uni.setStorageSync('cart_selected', this.selected)
    },

    /**
     * 加入购物车
     * @param {object} payload { goodsId, skuId, shopId, shopName, title, cover, specText, price, stock }
     * @param {number} quantity
     */
    addItem(payload, quantity = 1) {
      const exist = this.items.find((i) => i.skuId === payload.skuId)
      if (exist) {
        // 库存边界：不能超过实际库存
        exist.quantity = Math.min(exist.quantity + quantity, exist.stock)
        exist.status = exist.stock > 0 ? 'valid' : 'invalid'
      } else {
        this.items.push({
          ...payload,
          quantity,
          status: payload.stock > 0 ? 'valid' : 'invalid'
        })
      }
      this.selected[payload.skuId] = true
      this._persist()
    },

    /** 更新数量（库存边界校验） */
    updateQuantity(skuId, quantity) {
      const item = this.items.find((i) => i.skuId === skuId)
      if (!item) return
      if (quantity < 1) quantity = 1
      if (quantity > item.stock) {
        uni.showToast({ title: `最多可购买${item.stock}件`, icon: 'none' })
        quantity = item.stock
      }
      item.quantity = quantity
      this._persist()
    },

    /** 切换单个商品选中（失效商品不可选） */
    toggleItem(skuId) {
      const item = this.items.find((i) => i.skuId === skuId)
      if (!item || item.status !== 'valid') return
      this.selected[skuId] = !this.selected[skuId]
      this._persist()
    },

    /** 切换店铺选中（级联：全选/全不选该店所有有效商品） */
    toggleShop(shopId) {
      const items = this.validItems.filter((i) => i.shopId === shopId)
      const allSelected = items.every((i) => this.selected[i.skuId])
      items.forEach((i) => {
        this.selected[i.skuId] = !allSelected
      })
      this._persist()
    },

    /** 全选/取消全选（仅作用于有效商品） */
    toggleAll() {
      const next = !this.isAllSelected
      this.validItems.forEach((i) => {
        this.selected[i.skuId] = next
      })
      this._persist()
    },

    removeItem(skuId) {
      this.items = this.items.filter((i) => i.skuId !== skuId)
      delete this.selected[skuId]
      this._persist()
    },

    clearSelected() {
      const ids = this.selectedItems.map((i) => i.skuId)
      this.items = this.items.filter((i) => !ids.includes(i.skuId))
      ids.forEach((id) => delete this.selected[id])
      this._persist()
    },

    /**
     * 刷新商品状态（下架/库存归零/规格失效 -> 移入失效分组）
     * 拉取最新商品数据比对本地购物车
     */
    async refreshValidity() {
      try {
        const res = await goodsApi.list({})
        const goodsMap = {}
        res.data.list.forEach((g) => (goodsMap[g.id] = g))
        this.items.forEach((item) => {
          const g = goodsMap[item.goodsId]
          if (!g) {
            item.status = 'invalid'
            item.invalidReason = '商品已失效'
            return
          }
          if (g.status !== 'on') {
            item.status = 'invalid'
            item.invalidReason = '商品已下架'
            this.selected[item.skuId] = false
            return
          }
          const sku = g.skus.find((s) => s.id === item.skuId)
          if (!sku) {
            item.status = 'invalid'
            item.invalidReason = '规格已失效'
            this.selected[item.skuId] = false
            return
          }
          item.stock = sku.stock
          item.price = sku.price
          if (sku.stock <= 0) {
            item.status = 'invalid'
            item.invalidReason = '商品已售罄'
            this.selected[item.skuId] = false
          } else {
            item.status = 'valid'
            if (item.quantity > sku.stock) item.quantity = sku.stock
          }
        })
        this._persist()
      } catch (e) {
        // 忽略
      }
    },

    /**
     * 结算前二次库存拦截（后端库存临时变动）
     * @returns {Promise<boolean>} 是否可结算
     */
    async checkStockBeforeCheckout() {
      const items = this.selectedItems.map((i) => ({ skuId: i.skuId, quantity: i.quantity }))
      if (!items.length) return false
      const res = await stockApi.check(items)
      if (!res.data.valid) {
        // 标记失效并提示
        res.data.invalid.forEach((inv) => {
          const item = this.items.find((i) => i.skuId === inv.skuId)
          if (item) {
            item.status = 'invalid'
            item.invalidReason = inv.reason
            this.selected[item.skuId] = false
          }
        })
        this._persist()
        uni.showToast({
          title: res.data.invalid[0].reason,
          icon: 'none'
        })
        return false
      }
      return true
    }
  }
})
