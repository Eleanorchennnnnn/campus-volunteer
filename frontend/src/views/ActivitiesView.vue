<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'

const types = ref([])
const typeId = ref(null)
const activities = ref([])
const loading = ref(true)

const STATUS = { 0: ['草稿', 'gray'], 1: ['报名中', 'blue'], 2: ['进行中', 'green'], 3: ['已结束', 'gray'], 4: ['已取消', 'red'] }

function fmt(dt) {
  if (!dt) return ''
  return String(dt).replace('T', ' ').slice(0, 16)
}

async function loadTypes() {
  try {
    const res = await request.get('/activity-type/list')
    types.value = res.data || []
    if (types.value.length > 0) {
      typeId.value = types.value[0].id
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadActivities() {
  if (!typeId.value) return
  loading.value = true
  try {
    const res = await request.get('/activity/of/type', {
      params: { typeId: typeId.value, current: 1 }
    })
    activities.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function chooseType(id) {
  typeId.value = id
  loadActivities()
}

onMounted(async () => {
  await loadTypes()
  await loadActivities()
})
</script>

<template>
  <div class="container">
    <h1 class="page-title">校园活动</h1>

    <div class="card" style="display: flex; gap: 10px; flex-wrap: wrap; padding: 12px 16px;">
      <button v-for="t in types" :key="t.id" class="btn small"
              :class="{ ghost: typeId !== t.id }" @click="chooseType(t.id)">
        {{ t.name }}
      </button>
    </div>

    <div v-if="loading" class="empty">加载中...</div>
    <div v-else-if="activities.length === 0" class="empty">该分类暂无可报名的活动</div>

    <div class="grid">
      <router-link v-for="a in activities" :key="a.id" :to="'/activity/' + a.id" class="card" style="display: block;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <span class="tag" :class="(STATUS[a.status] || ['未知', 'gray'])[1]">
            {{ (STATUS[a.status] || ['未知'])[0] }}
          </span>
          <span v-if="a.volunteerHours > 0" class="tag orange">{{ a.volunteerHours }}h 志愿时长</span>
        </div>
        <h3 style="margin-bottom: 6px; font-size: 16px;">{{ a.title }}</h3>
        <p style="font-size: 13px; color: #888; margin-bottom: 4px;">{{ a.orgName }} · {{ a.typeName }}</p>
        <p style="font-size: 13px; color: #666; margin-bottom: 4px;">📍 {{ a.address }}</p>
        <p style="font-size: 13px; color: #666;">🕒 {{ fmt(a.beginTime) }} ~ {{ fmt(a.endTime) }}</p>
        <p v-if="a.totalQuota > 0" style="font-size: 13px; color: #1f6feb; margin-top: 6px;">
          剩余名额 {{ a.leftQuota }} / {{ a.totalQuota }}
        </p>
      </router-link>
    </div>
  </div>
</template>
