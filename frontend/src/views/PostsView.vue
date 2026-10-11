<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'
import { auth } from '../stores/auth'

const router = useRouter()
const posts = ref([])
const loading = ref(true)
const tip = ref('')
const tipType = ref('ok')

// 发帖表单
const form = ref({ title: '', content: '', activityId: '' })
const publishing = ref(false)

function fmt(dt) {
  if (!dt) return ''
  return String(dt).replace('T', ' ').slice(0, 16)
}

function showTip(text, type) {
  tip.value = text
  tipType.value = type
  setTimeout(() => { tip.value = '' }, 4000)
}

async function loadPosts() {
  try {
    const res = await request.get('/post/hot')
    posts.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function like(p) {
  if (!auth.token) {
    router.push('/login')
    return
  }
  try {
    const res = await request.put('/post/like/' + p.id)
    p.liked = res.data.likedCount
    p.isLiked = res.data.liked
  } catch (e) {
    showTip(e.message, 'err')
  }
}

async function publish() {
  if (!form.value.content.trim()) {
    showTip('内容不能为空', 'err')
    return
  }
  publishing.value = true
  try {
    const body = {
      title: form.value.title.trim() || null,
      content: form.value.content.trim(),
      activityId: form.value.activityId ? Number(form.value.activityId) : null
    }
    await request.post('/post', body)
    showTip('发布成功', 'ok')
    form.value = { title: '', content: '', activityId: '' }
    await loadPosts()
  } catch (e) {
    showTip(e.message, 'err')
  } finally {
    publishing.value = false
  }
}

onMounted(loadPosts)
</script>

<template>
  <div class="container">
    <h1 class="page-title">志愿社区</h1>

    <div v-if="tip" class="msg" :class="tipType">{{ tip }}</div>

    <!-- 发帖 -->
    <div class="card">
      <template v-if="auth.token">
        <div class="field">
          <label>标题（可选）</label>
          <input v-model="form.title" placeholder="给帖子起个标题" />
        </div>
        <div class="field">
          <label>内容</label>
          <textarea v-model="form.content" rows="3" placeholder="分享你的活动经历或志愿心得..."></textarea>
        </div>
        <div class="field">
          <label>关联活动ID（可选，用于活动回顾帖）</label>
          <input v-model="form.activityId" placeholder="如 1" />
        </div>
        <button class="btn" :disabled="publishing" @click="publish">
          {{ publishing ? '发布中...' : '发布帖子' }}
        </button>
      </template>
      <template v-else>
        <p style="font-size: 14px; color: #888;">
          <router-link to="/login" style="color: #1f6feb;">登录</router-link> 后即可发帖分享你的志愿经历
        </p>
      </template>
    </div>

    <!-- 帖子列表 -->
    <div v-if="loading" class="empty">加载中...</div>
    <div v-else-if="posts.length === 0" class="empty">还没有帖子，来发第一篇吧</div>
    <div v-for="p in posts" :key="p.id" class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
        <b style="font-size: 15px;">{{ p.title || '（无标题）' }}</b>
        <span style="font-size: 13px; color: #999;">{{ p.name }} · {{ fmt(p.createTime) }}</span>
      </div>
      <p style="font-size: 14px; color: #555; line-height: 1.8;">{{ p.content }}</p>
      <button class="btn small" :class="{ ghost: !p.isLiked, danger: p.isLiked }"
              style="margin-top: 10px;" @click="like(p)">
        {{ p.isLiked ? '❤ 已赞' : '♡ 点赞' }} {{ p.liked }}
      </button>
    </div>
  </div>
</template>
