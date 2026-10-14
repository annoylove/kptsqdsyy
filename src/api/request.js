/**
 * HTTP 请求层：统一封装 uni.request
 * - 自动携带 Access Token
 * - 401 时无感刷新：挂起当前请求 -> Refresh Token 换新 Token -> 重试原请求
 * - 并发 401 只刷新一次，其余请求排队等待
 * Mock 模式下请求路由到 mock/server.js
 */
import { mockServer } from '@/mock/server'
import { useUserStore } from '@/store/user'

const USE_MOCK = false // 联调真实 Spring Boot 后端（H5 走 vite 代理 /api -> :8080）
const BASE_URL = '/api'

let isRefreshing = false
let refreshWaiters = [] // 等待刷新完成的请求队列 { resolve, reject }

function onRefreshed(newToken) {
  refreshWaiters.forEach((w) => {
    if (newToken) w.resolve(request({ ...w.options, _retried: true }))
    else w.reject(new Error('登录已过期'))
  })
  refreshWaiters = []
}

/**
 * 核心请求方法
 * @param {object} options { url, method, data, silent }
 */
export function request(options) {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    const doSend = (token) => {
      const header = { 'Content-Type': 'application/json' }
      if (token) header.Authorization = `Bearer ${token}`

      if (USE_MOCK) {
        // 模拟网络：路由到 mock 服务端
        mockServer({
          url: options.url,
          method: options.method || 'GET',
          data: options.data || {},
          header
        }).then(handleResponse).catch(reject)
        return
      }

      uni.request({
        url: BASE_URL + options.url,
        method: options.method || 'GET',
        data: options.data || {},
        header,
        success: handleResponse,
        fail: (err) => reject(err)
      })
    }

    const handleResponse = (res) => {
      const statusCode = res.statusCode || 200
      if (statusCode === 401) {
        // refresh 接口自身 401（Refresh Token 失效）-> 直接失败，不再触发刷新，避免递归
        if (options.skipAuthRetry) {
          reject(res.data || { message: '登录已过期' })
          return
        }
        // Access Token 失效 -> 无感刷新
        handle401(options).then(resolve).catch(reject)
        return
      }
      if (statusCode >= 200 && statusCode < 300) {
        resolve(res.data)
      } else {
        if (!options.silent) {
          uni.showToast({ title: res.data?.message || '请求失败', icon: 'none' })
        }
        reject(res.data || { message: '网络异常' })
      }
    }

    doSend(userStore.accessToken)
  })
}

/**
 * 401 处理：刷新 token 并重放原请求；多个 401 并发时只刷新一次
 */
function handle401(originalOptions) {
  const userStore = useUserStore()
  // 已用新 token 重试过仍 401：token 确实无效，直接拒绝，避免二次刷新递归
  if (originalOptions._retried) {
    userStore.logout()
    uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
    return Promise.reject(new Error('登录已过期'))
  }
  if (isRefreshing) {
    // 挂起，等待刷新结果：成功则重放原请求，失败则直接拒绝（避免空 token 重放死循环）
    return new Promise((resolve, reject) => {
      refreshWaiters.push({ options: originalOptions, resolve, reject })
    })
  }
  isRefreshing = true
  return userStore
    .refreshAccessToken()
    .then((newToken) => {
      isRefreshing = false
      onRefreshed(newToken)
      return request({ ...originalOptions, _retried: true })
    })
    .catch((err) => {
      isRefreshing = false
      onRefreshed(null)
      userStore.logout()
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      throw err
    })
}

export function get(url, data, options = {}) {
  return request({ url, method: 'GET', data, ...options })
}
export function post(url, data, options = {}) {
  return request({ url, method: 'POST', data, ...options })
}
