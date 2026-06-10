# 前端B开发说明

## 一、负责范围

本部分为智慧校园生活助手项目的前端B开发内容，主要负责管理端页面、管理端接口封装、本地假后端联调环境，以及管理端整体视觉和交互细节优化。

已完成的管理端页面包括：

- 管理端登录页
- 管理端基础布局，包括左侧菜单、退出登录、路由切换
- 仪表盘页面
- 用户管理页面
- 管理员创建页面
- 日志管理页面
- 公告管理页面
- 系统配置页面

当前前端已根据 `接口文档(V1.3).md` 更新接口路径、字段结构和本地 Mock 假后端。

## 二、主要功能

### 1. 管理端登录

登录页调用统一登录接口 `/auth/login`，由接口返回的 `role` 判断是否允许进入管理后台。当前本地假后端提供管理员账号：

```text
手机号：18800188000
密码：Aa123456
```

登录成功后，前端将用户信息、`token`、`refreshToken`、`expiresIn` 和 `forceChangePassword` 保存到 Pinia 状态中，并进入管理端页面。错误密码会返回错误提示，不能进入系统。

Axios 请求层已增加以下处理：

- 自动携带 `Authorization: Bearer {token}`。
- 遇到 401 且存在 `refreshToken` 时，调用 `/auth/refresh` 刷新 token。
- 处理 V1.3 中的 403 子类型：`account_frozen`、`temp_password_required`、`insufficient_permission`。

### 2. 仪表盘

仪表盘调用 `/admin/dashboard`，展示：

- 总注册用户数
- 今日活跃用户数 DAU
- 今日日程添加数
- 今日健康打卡数
- 用户性别比例饼图
- 各年级人数柱状图
- 院系注册人数条形图

图表使用 ECharts，鼠标悬停时可以查看对应数量。

### 3. 用户管理

用户管理调用：

```text
GET /admin/users
POST /admin/users/{userId}/freeze
POST /admin/users/{userId}/unfreeze
POST /admin/users/{userId}/reset_password
```

已实现：

- 按用户 ID、手机号、昵称搜索
- 按账号状态筛选
- 分页展示
- 冻结账号
- 解封账号
- 展示冻结原因和冻结时间
- 管理员人工重置用户密码

冻结账号时需要选择 V1.3 规定的 `reasonCode`。解封账号、重置密码前会弹出二次确认。

### 4. 管理员创建

管理员创建调用：

```text
POST /auth/send_verify_code
POST /admin/admins
```

已实现：

- 仅超级管理员可创建普通管理员。
- 创建时填写手机号、登录密码、昵称和短信验证码。
- 验证码类型使用 `admin_create`。
- 创建前弹出二次确认。

### 5. 日志管理

日志管理调用：

```text
GET /admin/logs/operation
GET /admin/logs/exception
DELETE /admin/logs/clean
```

已实现：

- 操作日志列表展示。
- 按操作类型、目标类型和日期筛选。
- 异常日志列表展示。
- 普通管理员不能查看异常日志。
- 超级管理员可清理日志，清理前弹出二次确认。

### 6. 公告管理

公告管理调用：

```text
GET /admin/announcements
POST /admin/announcements
PUT /admin/announcements/{id}/offline
```

已实现：

- 查看公告列表
- 按公告状态和公告类型筛选
- 发布系统公告或运动提醒
- 公告正文 500 字限制
- 下架公告
- 下架前二次确认
- 对齐 V1.3 的 `active / offline` 公告状态

### 7. 系统配置

系统配置调用：

```text
GET /admin/config
PUT /admin/config
```

已实现：

- 默认每周运动目标配置
- 学业压力指数权重配置
- 日志保留天数配置
- 异常打卡阈值配置
- 拖延症预警阈值配置
- 保存配置前二次确认

配置数据已按 V1.3 调整为：

```text
health.defaultExerciseTarget
api.apiWeights
system.logRetentionDays
system.abnormalCheckinThreshold
system.procrastinationThreshold
```

## 三、代码结构

主要目录如下：

```text
src/
  api/                 接口封装
  components/          通用组件
  constants/           枚举常量
  layouts/             页面布局
  router/              路由配置
  stores/              Pinia 状态管理
  styles/              全局样式
  views/               页面
mock/
  config.js            本地假后端配置
  server.js            本地假后端接口实现
```

核心文件说明：

```text
src/api/request.js
```

统一 Axios 实例，处理接口基础地址、token 注入和错误提示。

```text
src/api/admin.js
```

管理端接口封装。

```text
src/layouts/AdminLayout.vue
```

管理端公共布局。

```text
src/views/admin/Dashboard.vue
src/views/admin/Users.vue
src/views/admin/Admins.vue
src/views/admin/Logs.vue
src/views/admin/Announcements.vue
src/views/admin/Config.vue
```

管理端核心页面。

```text
mock/server.js
```

本地假后端，模拟 `接口文档(V1.3).md` 和 ER 图中的主要接口。

## 四、版本更新记录

### 4.1 第一次更新：管理端基础版本

第一次更新主要完成前端B负责的管理端基础开发，内容包括：

1. 搭建 Vue 3 + Vite 前端工程，引入 Element Plus、Axios、Vue Router、Pinia 和 ECharts。
2. 完成管理端登录页、管理端布局、左侧菜单和退出登录。
3. 完成仪表盘页面，展示总用户数、活跃用户数、日程添加数、健康打卡数和三个 ECharts 图表。
4. 完成用户管理页面，支持用户列表、搜索、状态筛选、冻结、解封和冻结原因展示。
5. 完成公告管理页面，支持公告列表、发布公告和下架公告。
6. 完成系统配置页面，支持默认运动目标和学业压力指数权重配置。
7. 搭建本地 Mock 假后端，用于没有真实后端时模拟接口联调。
8. 完成基础页面美化、交互优化和 `npm.cmd run build` 构建验证。

### 4.2 第二次更新：根据接口文档 V1.3 调整

本次更新主要根据后端同学提供的 `接口文档(V1.3).md` 调整：

1. 认证接口：管理端登录改为统一 `/auth/login`，并增加 `/auth/refresh`、`refreshToken` 和 403 `subCode` 处理。
2. 用户管理：冻结参数改为 `reasonCode`，用户列表展示 `freezeReasonCode`、`freezeReasonText`、`freezeTime`，并新增人工重置密码。
3. 管理员管理：新增超级管理员创建普通管理员页面，对接 `/admin/admins`。
4. 日志管理：新增操作日志、异常日志、日志清理页面，对接 V1.3 日志接口。
5. 公告管理：公告状态改为 `active / offline`，增加筛选和分页处理。
6. 系统配置：配置结构改为 `health / api / system` 三类，补充系统风控参数。
7. 本地假后端：补充并同步 V1.3 相关接口，便于没有真实后端时继续联调。

### 4.3 本次更新相对上次的主要区别

| 对比项 | 上次基础版本 | 本次 V1.3 更新后 |
|--------|--------------|------------------|
| 登录接口 | 使用旧的 `/admin/login` | 改为统一 `/auth/login`，根据 `role` 判断是否进入管理端 |
| Token 处理 | 只保存 `token` | 保存 `token`、`refreshToken`、`expiresIn`，并支持 401 自动刷新 |
| 错误处理 | 主要显示普通错误信息 | 增加 `account_frozen`、`temp_password_required`、`insufficient_permission` 处理 |
| 用户管理 | 支持列表、筛选、冻结、解封 | 增加重置密码；冻结字段改为 `reasonCode`、`freezeReasonText`、`freezeTime` |
| 管理员管理 | 暂无页面 | 新增超级管理员创建普通管理员页面 |
| 日志管理 | 暂无页面 | 新增操作日志、异常日志和日志清理页面 |
| 公告管理 | 基础发布、下架 | 增加筛选、分页；状态改为 `active / offline` |
| 系统配置 | 运动目标和 API 权重 | 改为 V1.3 分组结构，增加日志保留、异常打卡阈值、拖延预警阈值 |
| Mock 假后端 | 模拟基础管理端接口 | 补充 V1.3 新增接口和字段结构 |
| 文档说明 | 说明基础功能和运行方式 | 增加版本留痕、V1.3 改动说明和遗留问题更新 |

## 五、本地运行

安装依赖：

```powershell
npm.cmd install
```

启动假后端：

```powershell
npm.cmd run mock
```

启动前端：

```powershell
npm.cmd run dev
```

浏览器访问：

```text
http://localhost:5173
```

## 六、Mock 假后端说明

当前没有真实后端代码，因此使用 `mock/server.js` 提供本地假后端。前端 `.env.development` 中配置：

```text
VITE_API_BASE_URL=http://localhost:3001
```

可在 `mock/config.js` 中修改：

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

- `randomizeUsersOnStart` 为 `true` 时，每次重启假后端会生成不同用户数据。
- `randomizeDashboardOnRequest` 为 `true` 时，每次请求仪表盘会生成不同统计数据。

以后真实后端完成后，只需要将 `.env.development` 改成真实后端地址即可。

## 七、简单测试记录

已进行以下手动测试：

1. 启动 `npm.cmd run mock` 后，访问 `/admin/dashboard` 可以返回仪表盘数据。
2. 使用正确管理员账号登录，可以进入管理端。
3. 使用错误密码登录，会提示手机号或密码错误，不能进入管理端。
4. 仪表盘三个统计图可以正常显示，鼠标悬停可以显示数量。
5. 用户管理可以查看用户列表、按状态筛选、冻结账号、解封账号、重置密码。
6. 管理员创建页面可以发送模拟验证码并创建普通管理员。
7. 日志管理可以查看操作日志、异常日志，并进行模拟清理。
8. 公告管理可以发布公告、筛选公告，下架公告前会弹出确认框。
9. 系统配置可以读取 V1.3 分组配置，保存前会弹出确认框。
10. 执行 `npm.cmd run build`，项目可以成功构建。

## 八、目前遗留问题

以下问题需要后续结合后端接口继续完善：

1. 冻结账号后的“强制用户当前会话下线”需要真实后端 token 校验或会话机制支持。
2. 普通管理员和超级管理员的细粒度权限控制需要真实后端返回更完整的角色或权限信息。
3. 课程、日程、周报等学生端功能虽然 API 层已部分对齐 V1.3，但本次前端B重点仍是管理端页面。
4. 真实后端完成后，需要用 Apifox 或浏览器联调逐项核对字段和错误码。
