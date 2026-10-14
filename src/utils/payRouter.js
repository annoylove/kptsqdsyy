/**
 * 支付路由分发逻辑（需求 3.2）
 * 不同终端调起不同支付渠道，前端封装统一分发器。
 * Mock 环境下各分支均模拟成功回调，真实项目中替换 #ifdef 块内逻辑即可。
 */

/**
 * @param {string} orderNo 订单号
 * @param {number} amount 支付金额（元）
 * @param {object} options { channel: 'WECHAT' | 'ALIPAY' }
 * @returns {Promise<{ success: boolean, tradeNo: string }>}
 */
export function triggerPay(orderNo, amount, options = {}) {
  const channel = options.channel || 'WECHAT'
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    // 1. 请求后端获取微信支付参数 (timeStamp, nonceStr, package, paySign)
    // 2. 调用 uni.requestPayment 拉起小程序原生支付面板
    mockGetWxPayParams(orderNo).then((params) => {
      uni.requestPayment({
        provider: 'wxpay',
        ...params,
        success: () => resolve({ success: true, tradeNo: params.transactionId }),
        fail: (err) => reject(new Error('支付取消或失败'))
      })
    })
    // #endif

    // #ifdef H5
    const inWechat =
      typeof navigator !== 'undefined' &&
      /MicroMessenger/i.test(navigator.userAgent)
    if (inWechat && channel === 'WECHAT') {
      // 微信内浏览器：JS-SDK wx.chooseWXPay（mock 直接成功）
      setTimeout(
        () => resolve({ success: true, tradeNo: mockTradeNo() }),
        800
      )
    } else {
      // 外部浏览器：重定向至支付宝/微信 H5 支付中间页
      // 真实环境：window.location.href = res.h5Url
      setTimeout(
        () => resolve({ success: true, tradeNo: mockTradeNo() }),
        800
      )
    }
    // #endif

    // #ifdef APP-PLUS
    // 调用原生支付插件 (uni.requestPayment) 拉起微信/支付宝 APP 支付
    uni.requestPayment({
      provider: channel === 'ALIPAY' ? 'alipay' : 'wxpay',
      orderInfo: `${orderNo}|${amount}`,
      success: () => resolve({ success: true, tradeNo: mockTradeNo() }),
      fail: () => reject(new Error('支付取消或失败'))
    })
    // #endif
  })
}

function mockTradeNo() {
  return `MOCK${Date.now()}`
}

function mockGetWxPayParams(orderNo) {
  return new Promise((resolve) => {
    setTimeout(
      () =>
        resolve({
          timeStamp: String(Math.floor(Date.now() / 1000)),
          nonceStr: Math.random().toString(36).slice(2),
          package: `prepay_id=${orderNo}`,
          paySign: 'MOCK_SIGN',
          transactionId: mockTradeNo()
        }),
      300
    )
  })
}
