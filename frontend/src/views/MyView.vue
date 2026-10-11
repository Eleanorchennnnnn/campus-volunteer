<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { auth } from '../stores/auth'

const enrollments = ref([])
const profile = ref(null)
const cert = ref(null)
const loading = ref(true)

const ENROLL_STATUS = {
  0: ['已报名', 'blue'],
  1: ['已签到', 'green'],
  2: ['已完成', 'orange']
}

function fmt(dt) {
  if (!dt) return ''
  return String(dt).replace('T', ' ').slice(0, 16)
}

function stars(level) {
  if (level == null) return ''
  return '★'.repeat(level) + '☆'.repeat(4 - level)
}

async function cancel(e) {
  if (!confirm(`确定取消「${e.activityTitle}」的报名吗？名额将回补。`)) return
  try {
    await request.delete('/enrollment/' + e.activityId)
    await loadAll()
  } catch (err) {
    alert(err.message)
  }
}

async function sign(e) {
  try {
    const res = await request.post('/enrollment/sign/' + e.activityId)
    alert(res.data)
    await loadAll()
  } catch (err) {
    alert(err.message)
  }
}

async function loadAll() {
  try {
    const [enrollRes, profileRes] = await Promise.all([
      request.get('/enrollment/my'),
      request.get('/volunteer/me')
    ])
    enrollments.value = enrollRes.data || []
    profile.value = profileRes.data
    try {
      const certRes = await request.get('/volunteer/certificate')
      cert.value = certRes.data
    } catch (e) { /* 忽略 */ }
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="container">
    <h1 class="page-title">我的</h1>

    <div v-if="loading" class="empty">加载中...</div>

    <template v-else>
      <!-- 志愿档案 -->
      <div class="card" style="background: linear-gradient(135deg, #eaf1ff, #fff);">
        <b>{{ auth.user?.nickName }} 的志愿档案</b>
        <p style="margin-top: 8px; font-size: 14px; line-height: 2; color: #555;">
          累计时长：<b style="color:#1f6feb; font-size: 18px;">{{ profile?.totalHours ?? 0 }}</b> 小时 ·
          积分 {{ profile?.credits ?? 0 }} · 星级 <b style="color:#d97706">{{ stars(profile?.level ?? 0) }}</b> ·
          <template v-if="profile?.rank">榜上第 {{ profile.rank }} 名</template>
          <template v-else>暂未上榜</template>
        </p>
      </div>

      <!-- 报名列表 -->
      <h2 style="font-size: 16px; margin: 18px 0 10px;">我的报名（{{ enrollments.length }}）</h2>
      <div class="card">
        <div v-if="enrollments.length === 0" class="empty">
          还没有报名活动，去 <router-link to="/" style="color:#1f6feb">活动列表</router-link> 看看
        </div>
        <div v-for="e in enrollments" :key="e.id" class="row-item">
          <router-link :to="'/activity/' + e.activityId" style="flex: 1;">
            <b style="font-size: 14px;">{{ e.activityTitle }}</b>
            <p style="font-size: 12px; color: #999; margin-top: 3px;">
              {{ fmt(e.activityBeginTime) }} · {{ e.activityAddress }}
            </p>
          </router-link>
          <span class="tag" :class="(ENROLL_STATUS[e.status] || ['已取消','gray'])[1]">
            {{ (ENROLL_STATUS[e.status] || ['已取消'])[0] }}
          </span>
          <button v-if="e.status === 0" class="btn ghost small" @click="sign(e)">签到</button>
          <button v-if="e.status === 0" class="btn danger small" @click="cancel(e)">取消</button>
        </div>
      </div>

      <!-- 志愿服务证明 -->
      <div v-if="cert && cert.completedCount > 0" class="card" style="border: 1px dashed #c9dcff;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <b>志愿服务证明</b>
          <span class="tag blue">{{ cert.completedCount }} 次服务</span>
        </div>
        <p style="font-size: 13px; color: #666; margin: 10px 0; line-height: 1.8;">
          兹证明 <b>{{ cert.name }}</b>（{{ cert.studentNo }}）累计完成志愿服务
          <b style="color:#1f6feb;">{{ cert.totalHours }}</b> 小时，志愿积分 {{ cert.credits }}，
          达到 {{ stars(cert.level) }} 星志愿者标准。
        </p>
        <div v-for="(a, i) in cert.activities" :key="i" class="row-item" style="padding: 8px 2px;">
          <span style="flex:1; font-size: 13px;">{{ a.title }}</span>
          <span style="font-size: 12px; color: #999;">{{ a.orgName }}</span>
          <span style="font-size: 12px; color: #1f6feb;">{{ a.hours }}h</span>
        </div>
      </div>
    </template>
  </div>
</template>
