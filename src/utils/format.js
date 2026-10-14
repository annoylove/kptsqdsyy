/**
 * 通用格式化与工具函数
 */

/** 分转元，保留两位小数 */
export function fenToYuan(fen) {
  return (fen / 100).toFixed(2)
}

/** 元（数字）显示 */
export function yuan(price) {
  return Number(price).toFixed(2)
}

/** 相对时间：刚刚 / x分钟前 / x小时前 / x天前 / 日期 */
export function fromNow(timestamp) {
  const diff = Date.now() - timestamp
  const min = 60 * 1000
  const hour = 60 * min
  const day = 24 * hour
  if (diff < min) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / min)}分钟前`
  if (diff < day) return `${Math.floor(diff / hour)}小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)}天前`
  const d = new Date(timestamp)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function pad(n) {
  return n < 10 ? `0${n}` : `${n}`
}

/** 倒计时格式化：ms -> mm:ss 或 hh:mm:ss */
export function countdown(ms) {
  if (ms < 0) ms = 0
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${pad(h)}:${pad(m)}:${pad(sec)}`
  return `${pad(m)}:${pad(sec)}`
}

/** 简单唯一 ID */
export function uid(prefix = '') {
  return `${prefix}${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`
}
