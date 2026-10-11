<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../api/request'
import { setLogin } from '../stores/auth'

const router = useRouter()
const route = useRoute()

const studentNo = ref('')
const code = ref('')
const sending = ref(false)
const countdown = ref(0)
const loading = ref(false)
const tip = ref('')   // ok | err
const tipType = ref('ok')

function showTip(text, type) {
  tip.value = text
  tipType.value = type
}

async function sendCode() {
  if (!studentNo.value.trim()) {
    showTip('请输入学号', 'err')
    return
  }
  sending.value = true
  try {
    const res = await request.post('/user/code?studentNo=' + studentNo.value.trim())
    if (res.success) {
      showTip('验证码已发送（演示环境：请查看后端控制台日志中的验证码）', 'ok')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) clearInterval(timer)
      }, 1000)
    }
  } catch (e) {
    showTip(e.message, 'err')
  } finally {
    sending.value = false
  }
}

async function doLogin() {
  if (!studentNo.value.trim() || !code.value.trim()) {
    showTip('请输入学号和验证码', 'err')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/user/login', {
      studentNo: studentNo.value.trim(),
      code: code.value.trim()
    })
    const token = res.data
    // 先保存token（请求拦截器从localStorage读取），再拉取用户信息
    setLogin(token, { nickName: '', role: 0 })
    const me = await request.get('/user/me')
    setLogin(token, me.data)
    router.push(route.query.redirect || '/')
  } catch (e) {
    showTip(e.message, 'err')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="container" style="max-width: 420px; margin-top: 60px;">
    <div class="card">
      <h2 style="margin-bottom: 6px;">登录</h2>
      <p style="font-size: 13px; color: #999; margin-bottom: 18px;">高校活动报名与志愿平台</p>

      <div v-if="tip" class="msg" :class="tipType">{{ tip }}</div>

      <div class="field">
        <label>学号</label>
        <input v-model="studentNo" placeholder="如 20230001 / admin0000" @keyup.enter="doLogin" />
      </div>
      <div class="field">
        <label>验证码</label>
        <div style="display: flex; gap: 10px;">
          <input v-model="code" placeholder="6位数字" @keyup.enter="doLogin" />
          <button class="btn ghost" style="white-space: nowrap;" :disabled="sending || countdown > 0" @click="sendCode">
            {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
          </button>
        </div>
      </div>
      <button class="btn" style="width: 100%;" :disabled="loading" @click="doLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </button>

      <p style="font-size: 12px; color: #aaa; margin-top: 14px; line-height: 1.8;">
        演示账号：学生 20230001~20230005<br/>
        管理员 admin0000（可发布/管理活动）<br/>
        组织方 T2021001（可发布活动并认定时长）
      </p>
    </div>
  </div>
</template>
