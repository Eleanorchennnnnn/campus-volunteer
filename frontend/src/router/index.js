import { createRouter, createWebHistory } from 'vue-router'
import { auth } from '../stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  { path: '/', name: 'activities', component: () => import('../views/ActivitiesView.vue') },
  { path: '/activity/:id', name: 'activity-detail', component: () => import('../views/ActivityDetailView.vue') },
  { path: '/volunteer', name: 'volunteer', component: () => import('../views/VolunteerView.vue') },
  { path: '/posts', name: 'posts', component: () => import('../views/PostsView.vue') },
  { path: '/publish', name: 'publish', component: () => import('../views/PublishView.vue'), meta: { auth: true } },
  { path: '/mine', name: 'mine', component: () => import('../views/MyView.vue'), meta: { auth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：需要登录的页面未登录时跳转登录页
router.beforeEach((to) => {
  if (to.meta.auth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
