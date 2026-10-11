import axios from 'axios'

/**
 * axios 实例：
 * - baseURL /api 由 Vite 代理转发到后端（开发期无跨域）
 * - 请求拦截器自动携带 token（后端约定请求头 authorization）
 * - 响应拦截器统一拆包 Result{success,data,errorMsg}
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.authorization = token
  }
  return config
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && body.success === false) {
      return Promise.reject(new Error(body.errorMsg || '操作失败'))
    }
    return body
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      // 登录态失效，清理并跳转登录页
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new Error('请先登录'))
    }
    if (err.response && err.response.status === 403) {
      return Promise.reject(new Error('没有权限执行该操作'))
    }
    return Promise.reject(new Error('网络异常，请稍后重试'))
  }
)

export default request
