---
AIGC:
  ContentProducer: '001191110102MAD55U9H0F10002'
  ContentPropagator: '001191110102MAD55U9H0F10002'
  Label: '1'
  ProduceID: 'fedc148b-193d-42cd-bfac-093afa230568'
  PropagateID: 'fedc148b-193d-42cd-bfac-093afa230568'
  ReservedCode1: '34923135-7953-40af-a025-999bda96a315'
  ReservedCode2: '34923135-7953-40af-a025-999bda96a315'
---

# campus-volunteer-web 前端

高校活动报名与志愿平台的前端工程（Vue3 + Vite + Vue Router + Axios）。

## 快速开始

```bash
# 1.安装依赖（首次）
npm install

# 2.启动开发服务器（需先启动后端 CampusVolunteerApplication，默认 8081）
npm run dev
```

启动后浏览器访问 http://localhost:5173/

> 提示：如果 npm 在 PowerShell 中报"禁止运行脚本"，改用 `npm.cmd run dev` 或使用 IDEA 内置终端。

## 与后端的联调机制

开发期由 Vite 做代理（见 `vite.config.js`）：前端所有请求发往 `/api/**`，
自动转发到后端 `http://localhost:8081`（可用环境变量 `BACKEND` 覆盖），**无跨域问题**。

```bash
# 例：后端跑在别的端口/机器上
BACKEND=http://localhost:9000 npm run dev
```

## 生产构建

```bash
npm run build     # 产物在 dist/，可交给任意静态服务器/Nginx 托管
npm run preview   # 本地预览构建产物
```

生产部署时需将 `/api` 反向代理到后端（Nginx 配置示例）：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8081/;
}
```

## 目录结构

```
src/
├── main.js               入口
├── App.vue               外壳（顶部导航 + 登录态）
├── style.css             全局样式
├── api/request.js        axios封装（token自动携带/Result拆包/401跳登录）
├── stores/auth.js        登录态（localStorage持久化）
├── router/index.js       路由 + 登录守卫
└── views/
    ├── LoginView.vue          学号验证码登录（60s倒计时）
    ├── ActivitiesView.vue     活动列表（分类筛选）
    ├── ActivityDetailView.vue 活动详情 + 报名/取消/签到 + 回顾帖
    ├── VolunteerView.vue      志愿时长榜 + 我的档案
    ├── PostsView.vue          社区帖子 + 点赞 + 发帖
    ├── MyView.vue             我的报名 + 电子证明
    └── PublishView.vue        组织方发布活动（角色门槛）
```

## 演示账号

学生 `20230001`~`20230005` · 管理员 `admin0000` · 组织方 `T2021001`
（验证码打印在后端控制台日志中）

> AI生成