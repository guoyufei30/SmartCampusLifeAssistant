# 本地假后端

启动：

```powershell
npm.cmd run mock
```

前端 `.env.development` 已经指向：

```text
VITE_API_BASE_URL=http://localhost:3001
```

再开另一个终端启动前端：

```powershell
npm.cmd run dev
```

登录管理端可用：

```text
手机号：18800188000
密码：Aa123456
```

可以在 `mock/config.js` 修改本地假后端配置：

```js
export const mockConfig = {
  port: 3001,
  admin: {
    phone: '18800188000',
    password: 'Aa123456',
  },
  randomizeUsersOnStart: true,
  randomizeDashboardOnRequest: true,
}
```

说明：

- `randomizeUsersOnStart: true`：每次重启假后端时生成一批不同的用户。
- `randomizeDashboardOnRequest: true`：每次刷新仪表盘时返回不同的统计数据。

这个 Mock 服务按当前 `接口文档(V1.3).md` 和 ER 图字段实现，后续接真实后端时只需要把 `.env.development` 改成真实后端地址。

## V1.3 已同步的主要接口

本次更新相对上次 Mock 基础版本的区别：

- 上次主要模拟管理端登录、仪表盘、用户管理、公告管理和系统配置基础接口。
- 本次根据 `接口文档(V1.3).md` 增加统一登录、Token 刷新、强制改密、管理员创建、重置密码、日志管理、课程相关接口和新的配置结构。
- 用户冻结原因、公告状态、系统配置字段已经按 V1.3 调整。
- Mock 数据继续保留随机用户和随机仪表盘数据，便于前端演示。

账号认证：

```text
POST /auth/login
POST /auth/refresh
POST /auth/send_verify_code
POST /user/force_password
```

管理端：

```text
GET /admin/dashboard
GET /admin/users
POST /admin/users/{userId}/freeze
POST /admin/users/{userId}/unfreeze
POST /admin/users/{userId}/reset_password
POST /admin/admins
GET /admin/announcements
POST /admin/announcements
PUT /admin/announcements/{id}/offline
GET /admin/config
PUT /admin/config
GET /admin/logs/operation
GET /admin/logs/exception
DELETE /admin/logs/clean
```

课程与日程相关：

```text
GET /course/template
POST /course/import
GET /course/list
POST /course
PUT /course/{courseId}
DELETE /course/{courseId}
POST /schedule/events/check_conflict
```

说明：

- Mock 中的 token、验证码、日志清理和权限判断只用于前端联调演示，不代表真实安全实现。
- 真实后端完成后，需要重新核对接口字段、分页结构、错误码和权限返回。
