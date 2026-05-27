# 前端B开发说明

## 一、负责范围

本部分为智慧校园生活助手项目的前端B开发内容，主要负责管理端页面、管理端接口封装、本地假后端联调环境，以及管理端整体视觉和交互细节优化。

已完成的管理端页面包括：

- 管理端登录页
- 管理端基础布局，包括左侧菜单、退出登录、路由切换
- 仪表盘页面
- 用户管理页面
- 公告管理页面
- 系统配置页面

## 二、主要功能

### 1. 管理端登录

登录页调用 `/admin/login` 接口。当前本地假后端提供管理员账号：

```text
手机号：18800188000
密码：Aa123456
```

登录成功后，前端将用户信息和 token 保存到 Pinia 状态中，并进入管理端页面。错误密码会返回错误提示，不能进入系统。

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
```

已实现：

- 按用户 ID、手机号、昵称搜索
- 按账号状态筛选
- 分页展示
- 冻结账号
- 解封账号
- 展示冻结原因

冻结账号时需要选择冻结原因。解封账号前会弹出二次确认。

### 4. 公告管理

公告管理调用：

```text
GET /admin/announcements
POST /admin/announcements
PUT /admin/announcements/{id}/offline
```

已实现：

- 查看公告列表
- 发布系统公告或运动提醒
- 公告正文 500 字限制
- 下架公告
- 下架前二次确认

### 5. 系统配置

系统配置调用：

```text
GET /admin/config
PUT /admin/config
```

已实现：

- 默认每周运动目标配置
- 学业压力指数权重配置
- 保存配置前二次确认

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
src/views/admin/Announcements.vue
src/views/admin/Config.vue
```

管理端四个核心页面。

```text
mock/server.js
```

本地假后端，模拟当前接口文档和 ER 图中的主要接口。

## 四、本地运行

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

## 五、Mock 假后端说明

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

## 六、简单测试记录

已进行以下手动测试：

1. 启动 `npm.cmd run mock` 后，访问 `/admin/dashboard` 可以返回仪表盘数据。
2. 使用正确管理员账号登录，可以进入管理端。
3. 使用错误密码登录，会提示手机号或密码错误，不能进入管理端。
4. 仪表盘三个统计图可以正常显示，鼠标悬停可以显示数量。
5. 用户管理可以查看用户列表、按状态筛选、冻结账号、解封账号。
6. 公告管理可以发布公告，下架公告前会弹出确认框。
7. 系统配置可以读取配置，保存前会弹出确认框。
8. 执行 `npm.cmd run build`，项目可以成功构建。

## 七、目前遗留问题

以下问题需要后续结合后端接口继续完善：

1. 接口文档中管理员创建接口缺失，暂未实现“超级管理员创建普通管理员”功能。
2. 需求说明书和 ER 图中有异常日志、管理日志内容，但接口文档缺少日志查询接口，暂未实现日志管理页面。
3. 冻结账号后的“强制用户当前会话下线”需要真实后端 token 校验或会话机制支持。
4. 普通管理员和超级管理员的细粒度权限控制需要后端返回更完整的角色或权限信息。
5. 公告列表接口文档缺少明确响应示例，当前前端按 `id/content/type/status/createTime` 字段实现。
