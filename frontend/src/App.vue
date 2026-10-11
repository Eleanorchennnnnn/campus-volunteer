<script setup>
import { useRouter } from 'vue-router'
import { auth, clearLogin } from './stores/auth'
import request from './api/request'

const router = useRouter()

async function logout() {
  try {
    await request.post('/user/logout')
  } catch (e) {
    // 后端token已过期等场景忽略，本地照常清理
  }
  clearLogin()
  router.push('/login')
}
</script>

<template>
  <div class="navbar">
    <div class="inner">
      <router-link to="/" class="brand">校园志愿</router-link>
      <div class="nav-links">
        <router-link to="/">活动</router-link>
        <router-link to="/volunteer">志愿榜</router-link>
        <router-link to="/posts">社区</router-link>
        <router-link v-if="auth.user && auth.user.role >= 1" to="/publish">发布活动</router-link>
        <router-link v-if="auth.user" to="/mine">我的</router-link>
      </div>
      <template v-if="auth.user">
        <span style="font-size: 14px; color: #666">{{ auth.user.nickName }}</span>
        <button class="btn ghost small" @click="logout">退出</button>
      </template>
      <router-link v-else to="/login">
        <button class="btn small">登录</button>
      </router-link>
    </div>
  </div>
  <router-view />
</template>
