<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { auth } from '../stores/auth'

const rank = ref([])
const myProfile = ref(null)
const loading = ref(true)

function stars(level) {
  if (level == null) return ''
  return '★'.repeat(level) + '☆'.repeat(4 - level)
}

onMounted(async () => {
  try {
    const res = await request.get('/volunteer/rank')
    rank.value = res.data || []
    if (auth.token) {
      try {
        const me = await request.get('/volunteer/me')
        myProfile.value = me.data
      } catch (e) { /* 忽略 */ }
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container">
    <h1 class="page-title">志愿时长榜</h1>

    <div v-if="myProfile" class="card" style="background: linear-gradient(135deg, #eaf1ff, #fff);">
      <b>{{ auth.user?.nickName }}，你当前累计志愿服务</b>
      <p style="margin-top: 8px; font-size: 14px; line-height: 2; color: #555;">
        累计时长：<b style="color:#1f6feb">{{ myProfile.totalHours }}</b> 小时 ·
        积分 <b>{{ myProfile.credits }}</b> ·
        星级 <b style="color:#d97706">{{ stars(myProfile.level) }}</b> ·
        排名 <b v-if="myProfile.rank">第 {{ myProfile.rank }} 名</b><span v-else>未上榜</span>
      </p>
      <p style="font-size: 13px; color: #999;">完成志愿服务后，组织方认定时长将自动累计，可前往「我的」查看电子证明</p>
    </div>

    <div class="card">
      <div v-if="loading" class="empty">加载中...</div>
      <div v-else-if="rank.length === 0" class="empty">暂无上榜志愿者</div>
      <div v-for="r in rank" :key="r.userId" class="row-item">
        <span style="width: 36px; font-weight: 700; font-size: 16px;"
              :style="{ color: r.rank <= 3 ? '#d97706' : '#999' }">{{ r.rank }}</span>
        <span style="flex: 1;">{{ r.nickName }}</span>
        <span style="font-size: 13px; color: #d97706;">{{ stars(r.level) }}</span>
        <span style="width: 90px; text-align: right; font-weight: 600;">{{ r.hours }} h</span>
      </div>
    </div>
  </div>
</template>
