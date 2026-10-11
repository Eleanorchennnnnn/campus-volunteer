<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'
import { auth, isOrg } from '../stores/auth'

const router = useRouter()

// 组织列表（发布时选择主办方）
const orgs = ref([])
const orgLoading = ref(false)

const form = ref({
  orgId: 1,
  typeId: 3,
  title: '',
  address: '',
  content: '',
  beginTime: '',
  endTime: '',
  enrollEndTime: '',
  totalQuota: 20,
  volunteerHours: 3
})
const publishing = ref(false)
const tip = ref('')
const tipType = ref('ok')

function showTip(text, type) {
  tip.value = text
  tipType.value = type
}

async function loadOrgs() {
  orgLoading.value = true
  try {
    const res = await request.get('/activity-type/list')
    // 说明：组织列表暂无公开接口，演示环境固定为种子组织
    orgs.value = [
      { id: 1, name: '青年志愿者协会' },
      { id: 2, name: '校学生会文体部' },
      { id: 3, name: '计算机科技协会' }
    ]
  } finally {
    orgLoading.value = false
  }
}
loadOrgs()

async function publish() {
  const f = form.value
  if (!f.title.trim() || !f.beginTime || !f.endTime) {
    showTip('标题、开始时间、结束时间为必填', 'err')
    return
  }
  publishing.value = true
  try {
    // datetime-local 的值格式为 yyyy-MM-ddTHH:mm，后端可直接解析
    const body = {
      orgId: Number(f.orgId),
      typeId: Number(f.typeId),
      title: f.title.trim(),
      address: f.address.trim() || null,
      content: f.content.trim() || null,
      beginTime: f.beginTime,
      endTime: f.endTime,
      enrollEndTime: f.enrollEndTime || null,
      totalQuota: Number(f.totalQuota) || 0,
      volunteerHours: Number(f.volunteerHours) || 0
    }
    const res = await request.post('/org/activity', body)
    showTip(`发布成功！活动ID：${res.data}`, 'ok')
    setTimeout(() => router.push('/activity/' + res.data), 1200)
  } catch (e) {
    showTip(e.message, 'err')
  } finally {
    publishing.value = false
  }
}
</script>

<template>
  <div class="container" style="max-width: 640px;">
    <h1 class="page-title">发布活动</h1>

    <!-- 角色门槛提示 -->
    <div v-if="!isOrg()" class="msg err">
      发布活动需要「组织方」或「管理员」角色。
      当前账号 {{ auth.user?.nickName }} 是学生角色，可用演示账号 admin0000 或 T2021001 登录体验。
    </div>

    <template v-else>
      <div v-if="tip" class="msg" :class="tipType">{{ tip }}</div>

      <div class="card">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
          <div class="field">
            <label>主办方组织</label>
            <select v-model="form.orgId">
              <option v-for="o in orgs" :key="o.id" :value="o.id">{{ o.name }}</option>
            </select>
          </div>
          <div class="field">
            <label>活动分类</label>
            <select v-model="form.typeId">
              <option :value="1">讲座沙龙</option>
              <option :value="2">文体竞赛</option>
              <option :value="3">公益志愿</option>
              <option :value="4">社会实践</option>
            </select>
          </div>
        </div>

        <div class="field">
          <label>活动标题 *</label>
          <input v-model="form.title" placeholder="如：图书馆图书整理志愿服务" />
        </div>
        <div class="field">
          <label>活动地点</label>
          <input v-model="form.address" placeholder="如：校图书馆一层" />
        </div>
        <div class="field">
          <label>活动详情</label>
          <textarea v-model="form.content" rows="3" placeholder="活动内容介绍"></textarea>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
          <div class="field">
            <label>开始时间 *</label>
            <input v-model="form.beginTime" type="datetime-local" />
          </div>
          <div class="field">
            <label>结束时间 *</label>
            <input v-model="form.endTime" type="datetime-local" />
          </div>
        </div>
        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px;">
          <div class="field">
            <label>报名截止</label>
            <input v-model="form.enrollEndTime" type="datetime-local" />
          </div>
          <div class="field">
            <label>总名额（0=不限）</label>
            <input v-model="form.totalQuota" type="number" min="0" />
          </div>
          <div class="field">
            <label>志愿时长（小时）</label>
            <input v-model="form.volunteerHours" type="number" min="0" step="0.5" />
          </div>
        </div>

        <button class="btn" style="width: 100%;" :disabled="publishing" @click="publish">
          {{ publishing ? '发布中...' : '发布活动' }}
        </button>
      </div>
    </template>
  </div>
</template>
