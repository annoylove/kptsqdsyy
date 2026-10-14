/**
 * 用户 Store：登录态、Token 管理（支持无感刷新）
 */
import { defineStore } from 'pinia'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: uni.getStorageSync('user_info') || null,
    accessToken: uni.getStorageSync('access_token') || '',
    refreshToken: uni.getStorageSync('refresh_token') || ''
  }),
  getters: {
    isLogin: (state) => !!state.accessToken
  },
  actions: {
    async login(phone, code) {
      const res = await authApi.login({ phone, code })
      const { accessToken, refreshToken, user } = res.data
      this._setTokens(accessToken, refreshToken)
      this.user = user
      uni.setStorageSync('user_info', user)
      return user
    },
    /** 注册：手机号 + 验证码 + 昵称，成功后直接进入登录态 */
    async register(phone, code, nickname) {
      const res = await authApi.register({ phone, code, nickname })
      const { accessToken, refreshToken, user } = res.data
      this._setTokens(accessToken, refreshToken)
      this.user = user
      uni.setStorageSync('user_info', user)
      return user
    },
    /** 刷新 Token（401 拦截器调用），返回新的 accessToken
     *  注意：action 名不能与 state 的 refreshToken 同名，否则 Pinia 会相互覆盖 */
    async refreshAccessToken() {
      const res = await authApi.refresh(this.refreshToken)
      const { accessToken, refreshToken } = res.data
      this._setTokens(accessToken, refreshToken)
      return accessToken
    },
    _setTokens(accessToken, refreshToken) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      uni.setStorageSync('access_token', accessToken)
      uni.setStorageSync('refresh_token', refreshToken)
    },
    logout() {
      this.user = null
      this._setTokens('', '')
      uni.removeStorageSync('user_info')
      // 清除订单缓存，避免下次未登录时显示上次账号的订单
      import('./order').then(({ useOrderStore }) => useOrderStore().clear())
    },
    /** 未登录则跳转登录页，返回是否已登录 */
    requireLogin() {
      if (this.isLogin) return true
      uni.navigateTo({ url: '/pages/login/login' })
      return false
    }
  }
})
