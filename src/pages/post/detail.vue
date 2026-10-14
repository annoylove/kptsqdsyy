<template>
  <view class="page" v-if="post">
    <nav-bar :title="post.title" />
    <scroll-view scroll-y class="detail-scroll" :style="{ height: 'calc(100vh - ' + navHeight + 'px)' }">
      <view class="author-row">
        <image class="avatar" :src="post.author.avatar" />
        <view class="author-info">
          <text class="name">{{ post.author.nickname }}</text>
          <text class="time">{{ fromNow(post.createTime) }}</text>
        </view>
      </view>
      <text class="title">{{ post.title }}</text>
      <text class="content">{{ post.content }}</text>
      <view class="images">
        <image
          v-for="(img, i) in post.images"
          :key="i"
          class="img"
          :src="img"
          mode="aspectFill"
          @tap="preview(i)"
        />
      </view>
      <view class="foot">
        <text class="like press-effect" :class="{ liked: post.liked }" @tap="like">
          ♥ {{ post.likes }}
        </text>
        <text class="comment">💬 {{ post.comments }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { postApi } from '@/api'
import { fromNow } from '@/utils/format'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const post = ref(null)
const navHeight = (uni.getSystemInfoSync().statusBarHeight || 0) + 44

onLoad(async (query) => {
  const res = await postApi.detail(query.id)
  post.value = res.data
})

function preview(i) {
  uni.previewImage({ urls: post.value.images, current: i })
}

async function like() {
  if (!userStore.requireLogin()) return
  post.value.liked = !post.value.liked
  const res = await postApi.like(post.value.id, post.value.liked)
  post.value.likes = res.data.likes
}
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg-white;
}
.detail-scroll {
  padding: 32rpx;
  box-sizing: border-box;
}
.author-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  border: 2rpx solid $border-color;
}
.author-info {
  flex: 1;
}
.name {
  font-size: 28rpx;
  font-weight: 700;
  display: block;
}
.time {
  font-size: 22rpx;
  color: $text-tertiary;
  margin-top: 4rpx;
}
.title {
  font-size: 40rpx;
  font-weight: 800;
  display: block;
  margin: 36rpx 0 24rpx;
  line-height: 1.4;
  letter-spacing: 0.5rpx;
}
.content {
  font-size: 29rpx;
  line-height: 1.9;
  color: #3a3a42;
  letter-spacing: 0.3rpx;
}
.images {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 32rpx;
}
.img {
  width: calc((100% - 24rpx) / 3);
  height: 220rpx;
  border-radius: $radius-md;
  background: $bg-gray;
}
.foot {
  display: flex;
  gap: 48rpx;
  margin-top: 40rpx;
  padding-top: 32rpx;
  border-top: 1rpx solid $divider-color;
  font-size: 28rpx;
  color: $text-tertiary;
}
.foot .like {
  transition: all 0.2s ease;
}
.like.liked {
  color: $primary-color;
  font-weight: 600;
}
</style>
