<template>
  <view class="page">
    <view class="logo-area">
      <view class="logo"><text>🛍️</text></view>
      <text class="app-name">社区电商</text>
    </view>

    <view class="form">
      <view class="field">
        <text class="label">手机号</text>
        <input
          class="input"
          v-model="phone"
          type="number"
          maxlength="11"
          placeholder="演示账号：13800008888"
        />
      </view>
      <view class="field">
        <text class="label">验证码</text>
        <input
          class="input"
          v-model="code"
          type="number"
          maxlength="4"
          placeholder="演示验证码：8888"
        />
        <text class="code-btn" @tap="sendCode">{{ codeText }}</text>
      </view>
      <view class="login-btn press-effect" :class="{ dis: !canSubmit || loading }" @tap="submit">
        {{ loading ? '登录中…' : '登录' }}
      </view>
      <view class="to-register">
        <text class="link" @tap="goRegister">还没有账号？立即注册 ›</text>
      </view>
      <text class="tip">演示账号：13800008888，验证码：8888（点击获取验证码自动填充）</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const phone = ref('13800008888')
const code = ref('')
const loading = ref(false)
const codeText = ref('获取验证码')
const counting = ref(0)

const canSubmit = computed(() => /^1\d{10}$/.test(phone.value) && code.value.length >= 4)

function sendCode() {
  if (counting.value > 0) return
  counting.value = 60
  codeText.value = '60s'
  const t = setInterval(() => {
    counting.value--
    codeText.value = counting.value > 0 ? `${counting.value}s` : '获取验证码'
    if (counting.value <= 0) clearInterval(t)
  }, 1000)
  code.value = '8888' // 演示环境自动填充
  uni.showToast({ title: '验证码已发送（演示：8888）', icon: 'none' })
}

async function submit() {
  if (!canSubmit.value || loading.value) return
  loading.value = true
  try {
    await userStore.login(phone.value, code.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/login/register' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff5f5 0%, $bg-color 40%);
  padding: 0 48rpx;
  position: relative;
  overflow: hidden;
}
.page::before {
  content: '';
  position: absolute;
  width: 480rpx;
  height: 480rpx;
  border-radius: 50%;
  background: rgba(255, 77, 79, 0.06);
  top: -160rpx;
  right: -160rpx;
}
.page::after {
  content: '';
  position: absolute;
  width: 300rpx;
  height: 300rpx;
  border-radius: 50%;
  background: rgba(255, 77, 79, 0.04);
  top: 200rpx;
  left: -120rpx;
}
.logo-area {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 0 90rpx;
}
.logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 40rpx;
  background: $primary-gradient;
  color: #fff;
  font-size: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16rpx 40rpx rgba(255, 77, 79, 0.3);
}
.app-name {
  font-size: 44rpx;
  font-weight: 800;
  margin-top: 28rpx;
  letter-spacing: 4rpx;
}
.form {
  position: relative;
  background: $bg-white;
  border-radius: $radius-xl;
  padding: 48rpx 40rpx;
  box-shadow: $shadow-card;
  border: 1rpx solid $border-color;
}
.field {
  display: flex;
  align-items: center;
  background: $bg-gray;
  border-radius: $radius-pill;
  padding: 8rpx 32rpx;
  gap: 20rpx;
  margin-bottom: 28rpx;
  border: 2rpx solid transparent;
  transition: border-color 0.2s ease;
}
.field:focus-within {
  border-color: rgba(255, 77, 79, 0.35);
  background: $bg-white;
}
.label {
  width: 110rpx;
  font-size: 26rpx;
  color: $text-secondary;
  font-weight: 500;
}
.input {
  flex: 1;
  font-size: 28rpx;
  padding: 20rpx 0;
}
.code-btn {
  font-size: 24rpx;
  color: $primary-color;
  padding: 10rpx 24rpx;
  border: 1rpx solid rgba(255, 77, 79, 0.4);
  border-radius: $radius-pill;
  white-space: nowrap;
  background: $bg-white;
}
.login-btn {
  margin-top: 48rpx;
  height: 92rpx;
  line-height: 92rpx;
  text-align: center;
  background: $primary-gradient;
  color: #fff;
  border-radius: $radius-pill;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
  box-shadow: $shadow-primary;
}
.login-btn.dis {
  background: $bg-gray;
  color: $text-placeholder;
  box-shadow: none;
}
.to-register {
  margin-top: 36rpx;
  text-align: center;
}
.link {
  font-size: 26rpx;
  color: $primary-color;
  font-weight: 500;
}
.tip {
  position: relative;
  display: block;
  margin-top: 40rpx;
  font-size: 22rpx;
  color: $text-tertiary;
  text-align: center;
}
</style>
