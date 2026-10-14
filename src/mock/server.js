/**
 * Mock 服务端：模拟真实后端接口
 * - 内存数据库（商品/帖子/订单/购物车）
 * - Token 校验：Access Token 过期返回 401，Refresh Token 换新
 * - 模拟网络延迟
 */
import { goodsList, shops, mockPosts, mockUser } from './data'
import { OrderStatus, OrderEvent, transition } from '@/utils/orderStateMachine'
import { uid } from '@/utils/format'

// ---- 内存数据库（页面刷新后由 store 从 storage 恢复） ----
const db = {
  goods: JSON.parse(JSON.stringify(goodsList)),
  shops,
  posts: JSON.parse(JSON.stringify(mockPosts)),
  orders: [],
  users: { '13800008888': { code: '8888', user: mockUser } }
}

// ---- Token 管理 ----
// accessToken 有效期 60s（便于演示 401 无感刷新），refreshToken 有效期 7 天
let validAccessTokens = new Set()
let validRefreshTokens = new Set()

function issueTokens() {
  const accessToken = uid('AT_')
  const refreshToken = uid('RT_')
  validAccessTokens.add(accessToken)
  validRefreshTokens.add(refreshToken)
  // 60 秒后 accessToken 失效，用于演示 401 -> 无感刷新
  setTimeout(() => validAccessTokens.delete(accessToken), 60 * 1000)
  return { accessToken, refreshToken }
}

function checkAuth(header) {
  const token = (header.Authorization || '').replace('Bearer ', '')
  if (!token) return false
  return validAccessTokens.has(token)
}

// ---- 模拟网络延迟 ----
function delay(ms = 300) {
  return new Promise((r) => setTimeout(r, ms + Math.random() * 200))
}

function ok(data) {
  return { statusCode: 200, data: { code: 0, message: 'ok', data } }
}
function fail(statusCode, message, code = -1) {
  return { statusCode, data: { code, message } }
}

// ---- 路由表 ----
const routes = {
  // 认证
  'POST /auth/login': async ({ data }) => {
    const account = db.users[data.phone]
    if (!account || account.code !== data.code) {
      return fail(400, '手机号或验证码错误')
    }
    const tokens = issueTokens()
    return ok({ ...tokens, user: account.user })
  },
  'POST /auth/refresh': async ({ data }) => {
    if (!validRefreshTokens.has(data.refreshToken)) {
      return fail(401, 'Refresh Token 无效')
    }
    // 旧 refreshToken 轮换
    validRefreshTokens.delete(data.refreshToken)
    const tokens = issueTokens()
    return ok(tokens)
  },

  // 商品
  'GET /goods/list': async ({ data }) => {
    let list = db.goods.filter((g) => g.status === 'on')
    if (data.keyword) {
      list = list.filter((g) => g.title.includes(data.keyword))
    }
    if (data.shopId) list = list.filter((g) => g.shopId === data.shopId)
    return ok({ list, total: list.length })
  },
  'GET /goods/detail': async ({ data }) => {
    const g = db.goods.find((x) => x.id === data.id)
    if (!g) return fail(404, '商品不存在')
    return ok(g)
  },
  'GET /shop/list': async () => ok(db.shops),

  // 社区
  'GET /post/list': async ({ data }) => {
    const page = data.page || 1
    const size = data.size || 10
    const start = (page - 1) * size
    return ok({
      list: db.posts.slice(start, start + size),
      hasMore: start + size < db.posts.length,
      total: db.posts.length
    })
  },
  'GET /post/detail': async ({ data }) => {
    const p = db.posts.find((x) => x.id === data.id)
    return p ? ok(p) : fail(404, '帖子不存在')
  },
  'POST /post/publish': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const post = {
      id: uid('P'),
      author: { nickname: mockUser.nickname, avatar: mockUser.avatar },
      title: data.title,
      content: data.content,
      images: data.images || [],
      likes: 0,
      comments: 0,
      createTime: Date.now(),
      liked: false
    }
    db.posts.unshift(post)
    return ok(post)
  },
  'POST /post/like': async ({ data }) => {
    const p = db.posts.find((x) => x.id === data.id)
    if (!p) return fail(404, '帖子不存在')
    p.liked = data.liked
    p.likes += data.liked ? 1 : -1
    return ok({ likes: p.likes, liked: p.liked })
  },

  // 库存校验（下单/结算时后端二次拦截）
  'POST /stock/check': async ({ data }) => {
    // data.items: [{ skuId, quantity }]
    const invalid = []
    data.items.forEach((item) => {
      for (const g of db.goods) {
        const sku = g.skus.find((s) => s.id === item.skuId)
        if (sku) {
          if (g.status !== 'on') invalid.push({ ...item, reason: '商品已下架' })
          else if (sku.stock < item.quantity)
            invalid.push({ ...item, reason: `库存不足，仅剩${sku.stock}件` })
        } else {
          invalid.push({ ...item, reason: '规格已失效' })
        }
      }
    })
    return ok({ valid: invalid.length === 0, invalid })
  },

  // 订单
  'POST /order/create': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const order = {
      id: uid('O'),
      orderNo: `NO${Date.now()}`,
      status: OrderStatus.PENDING_PAYMENT,
      items: data.items, // [{ goodsId, skuId, title, cover, specText, price, quantity, shopId, shopName }]
      totalAmount: data.items.reduce((s, i) => s + i.price * i.quantity, 0),
      createTime: Date.now(),
      payTime: null,
      shipTime: null,
      trackingNo: null,
      finishTime: null
    }
    db.orders.unshift(order)
    return ok(order)
  },
  'GET /order/list': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    let list = db.orders
    if (data.status) list = list.filter((o) => o.status === data.status)
    return ok({ list })
  },
  'GET /order/detail': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const o = db.orders.find((x) => x.orderNo === data.orderNo)
    return o ? ok(o) : fail(404, '订单不存在')
  },
  'POST /order/pay': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const o = db.orders.find((x) => x.orderNo === data.orderNo)
    if (!o) return fail(404, '订单不存在')
    try {
      o.status = transition(o.status, OrderEvent.PAY)
      o.payTime = Date.now()
    } catch (e) {
      return fail(400, e.message)
    }
    return ok(o)
  },
  'POST /order/cancel': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const o = db.orders.find((x) => x.orderNo === data.orderNo)
    if (!o) return fail(404, '订单不存在')
    try {
      o.status = transition(o.status, OrderEvent.CANCEL)
    } catch (e) {
      return fail(400, e.message)
    }
    return ok(o)
  },
  'POST /order/ship': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const o = db.orders.find((x) => x.orderNo === data.orderNo)
    if (!o) return fail(404, '订单不存在')
    try {
      o.status = transition(o.status, OrderEvent.SHIP)
      o.trackingNo = data.trackingNo
      o.shipTime = Date.now()
    } catch (e) {
      return fail(400, e.message)
    }
    return ok(o)
  },
  'POST /order/confirm': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const o = db.orders.find((x) => x.orderNo === data.orderNo)
    if (!o) return fail(404, '订单不存在')
    try {
      o.status = transition(o.status, OrderEvent.CONFIRM_RECEIPT)
      o.finishTime = Date.now()
    } catch (e) {
      return fail(400, e.message)
    }
    return ok(o)
  },
  'POST /order/refund/apply': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const o = db.orders.find((x) => x.orderNo === data.orderNo)
    if (!o) return fail(404, '订单不存在')
    try {
      o.status = transition(o.status, OrderEvent.APPLY_REFUND)
    } catch (e) {
      return fail(400, e.message)
    }
    return ok(o)
  },
  'POST /order/refund/agree': async ({ data, header }) => {
    if (!checkAuth(header)) return fail(401, 'Token 已过期')
    const o = db.orders.find((x) => x.orderNo === data.orderNo)
    if (!o) return fail(404, '订单不存在')
    try {
      o.status = transition(o.status, OrderEvent.AGREE_REFUND)
    } catch (e) {
      return fail(400, e.message)
    }
    return ok(o)
  },

  // 上传（模拟返回 URL）
  'POST /upload': async ({ data }) => {
    await delay(600)
    return ok({ url: data.url || `https://picsum.photos/seed/${uid()}/600/600` })
  }
}

/**
 * Mock 服务入口
 * @param {object} req { url, method, data, header }
 */
export async function mockServer(req) {
  await delay()
  const key = `${req.method} ${req.url}`
  const handler = routes[key]
  if (!handler) {
    return fail(404, `Mock 路由未定义: ${key}`)
  }
  return handler(req)
}

export { db, issueTokens }
