import { reactive } from 'vue'

/**
 * 登录态（模块级响应式单例，轻量场景不引入 pinia）
 */
export const auth = reactive({
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null')
})

export function setLogin(token, user) {
  auth.token = token
  auth.user = user
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify(user))
}

export function clearLogin() {
  auth.token = ''
  auth.user = null
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export function isAdmin() {
  return auth.user && auth.user.role === 2
}

export function isOrg() {
  return auth.user && auth.user.role >= 1
}
