<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../api/request'
import { auth } from '../stores/auth'

const route = useRoute()
const activityId = route.params.id

const activity = ref(null)
const myEnrollment = ref(null)   // 我对该活动的报名记录
const posts = ref([])
const loading = ref(true)
const acting = ref(false)
const tip = ref('')
const tipType = ref('ok')

const STATUS = { 0: ['草稿', 'gray'], 1: ['报名中', 'blue'], 2: ['进行中', 'green'], 3: ['已结束', 'gray'], 4: ['已取消', 'red'] }
const ENROLL_STATUS = { 0: '已报名', 1: '已签到', 2: '已完成' }

function fmt(dt) {
  if (!dt) return ''
  return String(dt).replace('T', ' ').slice(0, 16)
}

function showTip(text, type) {
  tip.value = text
  tipType.value = type
  setTimeout(() => { tip.value = '' }, 4000)
}

// 是否处于签到窗口（活动开始后~结束前）
const inSignWindow = computed(() => {
  if (!activity.value) return false
  const now = new Date()
  const begin = activity.value.beginTime ? new Date(activity.value.beginTime) : null
  const end = activity.value.endTime ? new Date(activity.value.endTime) : null
  return (!begin || now >= begin) && (!end || now <= end)
})

const canCancel = computed(() => {
  if (!activity.value || !myEnrollment.value) return false
  if (myEnrollment.value.status !== 0) return false
  const begin = activity.value.beginTime ? new Date(activity.value.beginTime) : null
  return !begin || new Date() < begin
})

async function loadActivity() {
  const res = await request.get('/activity/' + activityId)
  activity.value = res.data
}

async function loadMyEnrollment() {
  if (!auth.token) return
  try {
    const res = await request.get('/enrollment/my')
    myEnrollment.value = (res.data || []).find(e => e.activityId === Number(activityId)) || null
  } catch (e) { /* 未登录或异常忽略 */ }
}

async function loadPosts() {
  try {
    const res = await request.get('/post/of/activity/' + activityId)
    posts.value = res.data || []
  } catch (e) { /* 暂无帖子 */ }
}

async function enroll() {
  acting.value = true
  try {
    await request.post('/enrollment/' + activityId)
    showTip('报名成功！单号已生成', 'ok')
    await Promise.all([loadActivity(), loadMyEnrollment()])
  } catch (e) {
    showTip(e.message, 'err')
  } finally {
    acting.value = false
  }
}

async function cancel() {
  acting.value = true
  try {
    await request.delete('/enrollment/' + activityId)
    showTip('已取消报名，名额已回补', 'ok')
    await Promise.all([loadActivity(), loadMyEnrollment()])
  } catch (e) {
    showTip(e.message, 'err')
  } finally {
    acting.value = false
  }
}

async function sign() {
  acting.value = true
  try {
    const res = await request.post('/enrollment/sign/' + activityId)
    showTip(res.data, 'ok')
    await loadMyEnrollment()
  } catch (e) {
    showTip(e.message, 'err')
  } finally {
    acting.value = false
  }
}

onMounted(async () => {
  try {
    await loadActivity()
    await Promise.all([loadMyEnrollment(), loadPosts()])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container">
    <div v-if="loading" class="empty">加载中...</div>

    <template v-else-if="activity">
      <h1 class="page-title">{{ activity.title }}</h1>

      <div v-if="tip" class="msg" :class="tipType">{{ tip }}</div>

      <div class="card">
        <div style="display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap;">
          <span class="tag" :class="(STATUS[activity.status] || ['未知','gray'])[1]">
            {{ (STATUS[activity.status] || ['未知'])[0] }}
          </span>
          <span class="tag blue">{{ activity.orgName }}</span>
          <span class="tag gray">{{ activity.typeName }}</span>
          <span v-if="activity.volunteerHours > 0" class="tag orange">认定志愿时长 {{ activity.volunteerHours }}h</span>
        </div>

        <p style="line-height: 1.9; color: #555; font-size: 14px; margin-bottom: 14px;">
          📍 {{ activity.address }}<br/>
          🕒 活动时间：{{ fmt(activity.beginTime) }} ~ {{ fmt(activity.endTime) }}<br/>
          <template v-if="activity.enrollStartTime">📝 报名时间：{{ fmt(activity.enrollStartTime) }} ~ {{ fmt(activity.enrollEndTime) }}<br/></template>
          <template v-if="activity.totalQuota > 0">👥 名额：剩余 {{ activity.leftQuota }} / {{ activity.totalQuota }}</template>
          <template v-else>👥 名额：不限</template>
        </p>

        <p style="line-height: 1.9; color: #444; font-size: 14px; white-space: pre-wrap;">{{ activity.content }}</p>

        <div style="margin-top: 18px; border-top: 1px solid #f0f0f0; padding-top: 16px;">
          <!-- 未登录 -->
          <router-link v-if="!auth.token" to="/login">
            <button class="btn">登录后报名</button>
          </router-link>

          <!-- 已登录：按报名状态渲染 -->
          <template v-else>
            <!-- 无报名记录 -->
            <button v-if="!myEnrollment && activity.status === 1" class="btn success" :disabled="acting" @click="enroll">
              {{ acting ? '报名中...' : '立即报名' }}
            </button>

            <!-- 已报名 -->
            <template v-else-if="myEnrollment && myEnrollment.status === 0">
              <span class="tag blue" style="margin-right: 12px;">{{ ENROLL_STATUS[0] }}</span>
              <button v-if="canCancel" class="btn danger small" :disabled="acting" @click="cancel">取消报名</button>
              <button v-if="inSignWindow" class="btn success" style="margin-left: 8px;" :disabled="acting" @click="sign">
                现场签到
              </button>
            </template>

            <!-- 已签到 -->
            <template v-else-if="myEnrollment && myEnrollment.status === 1">
              <span class="tag green" style="margin-right: 12px;">{{ ENROLL_STATUS[1] }}</span>
              <span style="font-size: 13px; color: #999;">等待组织方结束活动并认定志愿时长</span>
            </template>

            <!-- 已完成 -->
            <template v-else-if="myEnrollment && myEnrollment.status === 2">
              <span class="tag orange" style="margin-right: 12px;">{{ ENROLL_STATUS[2] }}</span>
              <span style="font-size: 13px; color: #999;">志愿时长已认定，感谢参与！</span>
            </template>
          </template>
        </div>
      </div>

      <h2 style="font-size: 16px; margin: 18px 0 10px;">活动回顾帖（{{ posts.length }}）</h2>
      <div v-if="posts.length === 0" class="card empty" style="padding: 20px;">还没有回顾帖，去 <router-link to="/posts" style="color:#1f6feb">社区</router-link> 发一篇吧</div>
      <div v-for="p in posts" :key="p.id" class="card">
        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
          <b style="font-size: 15px;">{{ p.title || '（无标题）' }}</b>
          <span style="font-size: 13px; color: #999;">{{ p.name }}</span>
        </div>
        <p style="font-size: 14px; color: #555; line-height: 1.8;">{{ p.content }}</p>
        <p style="font-size: 12px; color: #aaa; margin-top: 8px;">❤ {{ p.liked }} · {{ fmt(p.createTime) }}</p>
      </div>
    </template>

    <div v-else class="empty">活动不存在</div>
  </div>
</template>
