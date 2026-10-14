/**
 * 业务 API 封装
 */
import { get, post } from './request'

export const authApi = {
  login: (data) => post('/auth/login', data),
  register: (data) => post('/auth/register', data),
  refresh: (refreshToken) => post('/auth/refresh', { refreshToken }, { skipAuthRetry: true })
}

export const goodsApi = {
  list: (params) => get('/goods/list', params),
  detail: (id) => get('/goods/detail', { id }),
  shops: () => get('/shop/list')
}

export const postApi = {
  list: (params) => get('/post/list', params),
  detail: (id) => get('/post/detail', { id }),
  publish: (data) => post('/post/publish', data),
  like: (id, liked) => post('/post/like', { id, liked })
}

export const orderApi = {
  create: (data) => post('/order/create', data),
  list: (params) => get('/order/list', params),
  detail: (orderNo) => get('/order/detail', { orderNo }),
  pay: (orderNo) => post('/order/pay', { orderNo }),
  cancel: (orderNo) => post('/order/cancel', { orderNo }),
  ship: (orderNo, trackingNo) => post('/order/ship', { orderNo, trackingNo }),
  confirm: (orderNo) => post('/order/confirm', { orderNo }),
  applyRefund: (orderNo) => post('/order/refund/apply', { orderNo }),
  agreeRefund: (orderNo) => post('/order/refund/agree', { orderNo })
}

export const stockApi = {
  check: (items) => post('/stock/check', { items })
}

export const uploadApi = {
  image: (url) => post('/upload', { url })
}
