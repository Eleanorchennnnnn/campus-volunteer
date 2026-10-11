---
AIGC:
  ContentProducer: '001191110102MAD55U9H0F10002'
  ContentPropagator: '001191110102MAD55U9H0F10002'
  Label: '1'
  ProduceID: '0cd33aab-6838-4f52-9509-dc9fe0b9ec9f'
  PropagateID: '0cd33aab-6838-4f52-9509-dc9fe0b9ec9f'
  ReservedCode1: 'a17a4288-df78-4376-bccb-0cb445e3cd1a'
  ReservedCode2: 'a17a4288-df78-4376-bccb-0cb445e3cd1a'
---

# campus-volunteer 高校活动报名与志愿平台

![Java](https://img.shields.io/badge/Java-8-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.12-green)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.4.3-blue)
![MySQL](https://img.shields.io/badge/MySQL-5.7%2B-4479A1)
![Redis](https://img.shields.io/badge/Redis-5%2B-dc382d)
![Vue3](https://img.shields.io/badge/Vue-3-42b883)
![Vite](https://img.shields.io/badge/Vite-5-646CFF)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

> 由黑马点评（hm-dianping）项目改造而来的校园场景全栈应用：学生在平台上浏览/报名校园活动与志愿服务，组织方发布活动、认定志愿时长，形成"报名 → 现场签到 → 时长认定 → 志愿榜单 → 电子证明"的完整闭环。

## 一、项目简介

平台三类角色：

| 角色 | 说明 | 权限 |
|---|---|---|
| 学生（role=0） | 浏览活动、抢报名、现场签到、发帖写心得 | 登录即可 |
| 组织方（role=1） | 发布/修改活动、结束活动并认定时长 | `/org/**` |
| 管理员（role=2） | 全部管理权限 | `/admin/**` |

核心业务闭环：

```
组织方发布活动(带名额/报名窗口/志愿时长)
   → 学生限量抢报名(Lua原子扣减, 一人一活动)
   → 活动当天现场签到(BitMap)
   → 组织方结束活动认定时长(累计画像/积分/星级, ZSet榜单)
   → 学生查看志愿档案/时长榜/电子证明
   → 社区沉淀: 活动回顾帖(Set一人一赞)/评论/关注组织
```

## 二、技术栈

- **后端**：Java 8（编译目标）/ Spring Boot 2.3.12 / MyBatis-Plus 3.4.3
- **前端**：Vue 3 + Vite + Vue Router + Axios（见 `frontend/` 目录）
- MySQL（活动/报名/用户画像持久层）
- Redis（会话、缓存、抢报名原子操作、签到 BitMap、时长榜 ZSet、点赞 Set）
- Hutool（工具库）/ Lombok

## 三、快速开始

### 环境要求

- JDK 17（编译目标为 Java 8，JDK 8/11/17 运行均可，不建议 JDK 24+）
- Node.js 18+（仅前端需要）
- MySQL 5.7+（本地 127.0.0.1:3306）
- Redis 5+（默认配置指向 `192.168.137.100:6379`，按需修改）

### 步骤

1. **初始化数据库**：执行 `src/main/resources/db/campus_volunteer.sql`
   （脚本会 DROP 并重建 `campus_volunteer` 库，含 10 张表与演示数据）
2. **配置数据库/Redis 连接**（二选一，仓库不含任何真实密码）：
   - 方式 A：复制 `application-local.yaml.example` 为 `application-local.yaml`，填入你的真实密码（该文件已被 .gitignore 忽略，不会误提交），并在 IDEA 运行配置的 Active profiles 填 `local`
   - 方式 B：启动时通过环境变量注入 `MYSQL_USERNAME` / `MYSQL_PASSWORD` / `REDIS_HOST` / `REDIS_PASSWORD`
3. **启动后端**：运行 `CampusVolunteerApplication`，默认端口 `8081`
4. **启动前端**：
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
5. **访问平台**：浏览器打开 http://localhost:5173/ （前端由 Vite 代理到后端，无跨域）

接口级验证可用 IDEA 打开根目录 `campus-api.http` 按说明逐步执行；页面截图见 `docs/screenshots/`。

### 演示账号

| 学号 | 角色 | 昵称 |
|---|---|---|
| 20230001 ~ 20230005 | 学生 | 陈小星 等 |
| admin0000 | 管理员 | 平台管理员 |
| T2021001 / T2021002 | 组织方 | 青志协·王老师 等 |

登录方式：学号 + 验证码（验证码打印在应用控制台日志中，`向 xxx 的手机 138... 发送验证码：425198`）。

## 四、接口文档

统一返回结构：`{"success": boolean, "data": ..., "errorMsg": ...}`；登录态通过请求头 `authorization: {token}` 传递。

### 用户与登录

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/user/code?studentNo=` | 发送验证码 | 公开 |
| POST | `/user/login` | 学号+验证码登录，返回 token | 公开 |
| POST | `/user/logout` | 登出（删除 Redis token） | 登录 |
| GET | `/user/me` | 当前登录用户（含 role） | 登录 |
| GET | `/user/info/{userId}` | 志愿者画像（时长/积分/星级） | 登录 |

### 活动浏览（公开）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/activity/{id}` | 活动详情（缓存三件套 + 富化组织名/分类名） |
| GET | `/activity/of/type?typeId=&current=` | 按分类分页（报名中/进行中） |
| GET | `/activity-type/list` | 分类列表（JSON 整体缓存） |

### 活动管理（组织方/管理员）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/org/activity` | 发布活动（限量活动自动预置 Redis 名额） |
| PUT | `/org/activity` | 修改活动（先更新库再删缓存） |
| POST | `/org/activity/{id}/finish` | 结束活动并认定志愿时长 |

### 报名（登录即可）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/enrollment/{activityId}` | 抢报名（Lua 原子校验扣减 + 事务落库） |
| DELETE | `/enrollment/{activityId}` | 取消报名（活动开始前，名额回补） |
| GET | `/enrollment/my` | 我的报名列表（富化活动信息） |
| POST | `/enrollment/sign/{activityId}` | 现场签到（BitMap，活动进行中） |

### 志愿档案

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/volunteer/rank` | 志愿时长榜 Top10 | 公开 |
| GET | `/volunteer/me` | 我的志愿档案（含排名） | 登录 |
| GET | `/volunteer/certificate` | 电子证明数据 | 登录 |

### 社区

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/post` | 发布帖子（可关联活动） | 登录 |
| GET | `/post/{id}` | 帖子详情（含点赞状态） | 公开 |
| PUT | `/post/like/{id}` | 点赞/取消（toggle） | 登录 |
| GET | `/post/hot` | 热门帖子 | 公开 |
| GET | `/post/of/me` | 我的帖子 | 登录 |
| GET | `/post/of/activity/{id}` | 活动回顾帖 | 公开 |
| POST | `/post-comment` | 发表评论（两级） | 登录 |
| GET | `/post-comment/of/{postId}` | 评论列表 | 公开 |
| DELETE | `/post-comment/{id}` | 删除自己的评论 | 登录 |
| PUT | `/follow/{orgId}` | 关注组织 | 登录 |
| DELETE | `/follow/{orgId}` | 取消关注 | 登录 |
| GET | `/follow/{orgId}` | 是否已关注 | 登录 |
| POST | `/upload/image` | 图片上传（返回 `/uploads/` URL） | 登录 |

## 五、Redis Key 设计

| Key | 类型 | 说明 | TTL |
|---|---|---|---|
| `login:code:{studentNo}` | String | 登录验证码 | 2 分钟 |
| `login:token:{token}` | Hash | 登录会话（id/nickName/icon/role） | 36000 分钟（访问续期） |
| `cache:activity:{id}` | String(JSON) | 活动详情缓存 | 30+0~9 分钟（随机防雪崩） |
| `cache:activity:{id}` = `""` | String | 空值标记（穿透防护） | 2 分钟 |
| `cache:activity:type:list` | String(JSON) | 分类列表整体缓存 | 1 天 |
| `lock:activity:{id}` | String | 缓存重建互斥锁（值=线程ID） | 10 秒 |
| `enroll:stock:{activityId}` | String | 剩余名额（Lua 原子扣减） | 长期 |
| `enroll:user:{activityId}` | Set | 已报名用户集合（一人一活动） | 长期 |
| `sign:activity:{activityId}` | BitMap | 现场签到（offset=用户ID） | 长期 |
| `volunteer:rank` | ZSet | 志愿时长榜（score=累计小时） | 长期 |
| `post:liked:{postId}` | Set | 帖子点赞用户集合（一人一赞） | 长期 |
| `icr:enroll:{yyyy:MM:dd}` | String INCR | 全局唯一报名单号序列 | 长期 |

## 六、核心技术方案

### 6.1 登录会话与三层拦截器

验证码存 Redis（2 分钟一次性使用）→ 登录成功后 UserDTO（含 role）以 Hash 存入 `login:token:{uuid}`。
拦截器三层：`RefreshTokenInterceptor`（刷新 TTL + ThreadLocal，始终放行）→ `LoginInterceptor`（无会话 401）→ `RoleInterceptor`（`/admin/**` 要求 role=2，`/org/**` 要求 role≥1，不足 403）。

### 6.2 活动详情缓存三件套

- **穿透**：数据库不存在的活动缓存空值 `""`（2 分钟），恶意 ID 打不到库
- **击穿**：缓存失效瞬间以 `setIfAbsent` 互斥锁保证单线程重建，其余线程休眠重试；持锁后双重检查；释放锁校验线程 ID 防误删
- **雪崩**：TTL = 30 分钟 + 随机 0~9 分钟，避免大量 key 同时失效
- 更新走"先更新库、再删缓存"，下次查询自动重建

### 6.3 限量抢报名（秒杀方案迁移）

```
窗口/状态校验 → Lua 脚本原子【查名额+查重+扣名额+记用户】
→ 事务落库【插入报名记录(全局ID) + 乐观锁扣DB名额(left_quota>0)】
→ 落库失败自动回补 Redis（补偿）
→ 冷启动兜底：Redis 名额/集合缺失时从数据库重建
```

一人一活动双层防护：Lua Set 校验 + `tb_enrollment` 唯一索引兜底。
并发实测：5 人并发抢 3 个名额，恰好 3 成功，Redis/DB 双边一致零超卖。

### 6.4 现场签到（BitMap）

`sign:activity:{id}` 以用户 ID 为 offset 记录签到，GETBIT 查重、SETBIT 记录、BITCOUNT 统计；配合报名状态流转 `0 已报名 → 1 已签到 → 2 已完成`。

### 6.5 志愿时长闭环

组织方结束活动 → 事务批量将已签到记录置完成 → 逐人累计画像（时长、5 分/小时积分、每 15 小时升 1 星封顶 4 星）→ ZSet `volunteer:rank` 累加 → 删除活动缓存。
关键顺序：榜单冷启动初始化必须放在画像更新**之前**，否则会双倍计数。

### 6.6 帖子点赞（Set 一人一赞）

`SADD/SREM post:liked:{id}` 一人一票，点赞/取消同一接口 toggle；计数冗余在 `tb_post.liked` 且扣减带 `gt(liked,0)` 防负数；点赞集合冷启动从历史表初始化，空集合用哨兵成员 `0` 标记避免反复查库。



## 七、二期规划

- GEO 附近活动（`activity:geo:` key 已预留，Shop 的 x/y 字段思路已迁移到活动表）
- 关注组织 Feed 推送（收件箱模式 ZSet）
- 活动 UV 统计（HyperLogLog）
- MQ 异步化报名落库（高并发版）
- 电子证明 PDF 渲染、管理端数据看板

## 八、项目结构

```
hm-dianping/
├── frontend/                前端工程（Vue3 + Vite，见 frontend/README.md）
├── docs/screenshots/        页面截图
├── campus-api.http          IDEA HTTP Client 全模块接口验证脚本
└── src/main/
    ├── java/com/campus/volunteer/
    │   ├── config/          拦截器(RefreshToken/Login/Role)、MvcConfig、MybatisConfig、全局异常
    │   ├── controller/      User/Activity/ActivityType/Org/Enrollment/Volunteer/Post/PostComment/Follow/Upload/Admin
    │   ├── service/         接口与实现（核心：ActivityServiceImpl缓存三件套、
    │   │                    EnrollmentServiceImpl抢报名、VolunteerServiceImpl时长闭环）
    │   ├── entity/          11张表实体
    │   ├── mapper/          MyBatis-Plus Mapper
    │   ├── dto/             Result/UserDTO/LoginFormDTO
    │   └── utils/           RedisIdWorker/RedisConstants/UserHolder/RegexUtils/PasswordEncoder
    └── resources/
        ├── db/campus_volunteer.sql     建库脚本（含演示数据）
        ├── lua/enroll.lua              抢报名原子脚本
        ├── mapper/                     MyBatis XML
        └── application.yaml            配置（upload.dir 可配图片目录）
```
