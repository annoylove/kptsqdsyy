/**
 * 智能压缩策略（F-Pub-2）
 * 单张图 > 2MB 时，使用 Canvas 等比压缩至最长边 1080px，质量 0.8。
 * 跨端：H5/APP 使用 canvas；小程序使用 uni.compressImage。
 */

const MAX_SIZE = 2 * 1024 * 1024 // 2MB
const MAX_EDGE = 1080 // 1080P
const QUALITY = 0.8

/**
 * @param {string} filePath 本地临时文件路径
 * @param {number} size 文件字节大小
 * @returns {Promise<string>} 压缩后的临时路径
 */
export function compressImage(filePath, size) {
  if (size <= MAX_SIZE) {
    return Promise.resolve(filePath)
  }
  // #ifdef MP-WEIXIN
  return new Promise((resolve) => {
    uni.compressImage({
      src: filePath,
      quality: Math.round(QUALITY * 100),
      success: (res) => resolve(res.tempFilePath),
      fail: () => resolve(filePath)
    })
  })
  // #endif

  // #ifdef H5 || APP-PLUS
  return canvasCompress(filePath).catch(() => filePath)
  // #endif
}

function canvasCompress(filePath) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      let { width, height } = img
      const scale = Math.min(1, MAX_EDGE / Math.max(width, height))
      width = Math.round(width * scale)
      height = Math.round(height * scale)
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, width, height)
      canvas.toBlob(
        (blob) => {
          if (!blob) return reject(new Error('压缩失败'))
          resolve(URL.createObjectURL(blob))
        },
        'image/jpeg',
        QUALITY
      )
    }
    img.onerror = reject
    img.src = filePath
  })
}
