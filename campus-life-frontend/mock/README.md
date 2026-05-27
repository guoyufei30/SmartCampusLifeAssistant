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

这个 Mock 服务按当前 `接口文档.md` 和 ER 图字段实现，后续接真实后端时只需要把 `.env.development` 改成真实后端地址。
