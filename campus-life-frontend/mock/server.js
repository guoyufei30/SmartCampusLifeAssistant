import http from 'node:http'
import { URL } from 'node:url'
import { mockConfig } from './config.js'

const PORT = mockConfig.port

// 以下工具函数用于生成每次启动时不同的模拟用户和仪表盘数据。
function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

function pick(list) {
  return list[randomInt(0, list.length - 1)]
}

function maskedPhone(prefix = '13') {
  return `${prefix}${randomInt(100000000, 999999999)}`.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}

const FREEZE_REASON_TEXT = {
  content_violation: '内容违规',
  security_risk: '安全风险',
  abnormal_checkin: '异常打卡',
  other: '其他',
}

function makeGeneratedUsers() {
  const departments = ['计算机学院', '软件学院', '自动化学院', '管理学院', '信息与电子学院', '机械与车辆学院']
  const grades = ['大一', '大二', '大三', '大四', '研究生']
  const names = ['艾比特', '李华', '王小北', '张晨', '刘思源', '陈一诺', '赵明远', '周亦航', '林可', '许安然']
  const count = randomInt(mockConfig.generatedUserCount.min, mockConfig.generatedUserCount.max)

  return Array.from({ length: count }, (_, index) => {
    const phone = maskedPhone(pick(['13', '15', '18']))
    const status = Math.random() > 0.82 ? 'frozen' : 'normal'

    return {
      userId: `usr_${String(index + 100).padStart(6, '0')}`,
      phone,
      rawPhone: phone.replace('****', String(randomInt(1000, 9999))),
      password: 'Aa123456',
      nickname: `${pick(names)}${randomInt(1, 99)}`,
      avatar: '',
      gender: pick(['男', '女', '未知']),
      grade: pick(grades),
      birthDate: '未设置',
      department: pick(departments),
      height: randomInt(158, 188),
      weight: Number((randomInt(500, 820) / 10).toFixed(1)),
      role: 'user',
      status,
      freezeReasonCode: status === 'frozen' ? pick(['content_violation', 'security_risk', 'abnormal_checkin']) : null,
      freezeReasonText: null,
      freezeTime: status === 'frozen' ? `2026-05-${String(randomInt(1, 27)).padStart(2, '0')} 14:30:00` : null,
      createTime: `2026-0${randomInt(1, 5)}-${String(randomInt(1, 27)).padStart(2, '0')} ${String(randomInt(8, 22)).padStart(2, '0')}:00:00`,
      lastLoginTime: `2026-05-${String(randomInt(1, 27)).padStart(2, '0')} ${String(randomInt(8, 22)).padStart(2, '0')}:30:00`,
    }
  })
}

// 仪表盘数据支持随机生成，便于演示图表刷新和接口联调效果。
function makeDashboardMetrics() {
  return {
    dailyActiveUsers: randomInt(800, 1800),
    dailyScheduleAdds: randomInt(180, 700),
    dailyHealthLogs: randomInt(350, 1200),
    genderDistribution: [
      { name: '男', value: randomInt(1800, 3200) },
      { name: '女', value: randomInt(1600, 3000) },
      { name: '未知', value: randomInt(80, 260) },
    ],
    gradeDistribution: [
      { name: '大一', value: randomInt(800, 1600) },
      { name: '大二', value: randomInt(800, 1600) },
      { name: '大三', value: randomInt(700, 1500) },
      { name: '大四', value: randomInt(600, 1300) },
      { name: '研究生', value: randomInt(200, 700) },
    ],
    departmentDistribution: [
      { name: '计算机学院', value: randomInt(500, 1000) },
      { name: '软件学院', value: randomInt(400, 900) },
      { name: '自动化学院', value: randomInt(300, 800) },
      { name: '管理学院', value: randomInt(260, 720) },
    ],
  }
}

// 内存数据源：字段尽量贴合接口文档和 ER 图，重启 Mock 服务后会恢复默认值。
const db = {
  users: [
    {
      userId: 'usr_000001',
      phone: '138****8000',
      rawPhone: '13800138000',
      password: 'Aa123456',
      nickname: '学霸小明',
      avatar: '',
      gender: '男',
      grade: '大二',
      birthDate: '2004-06-15',
      department: '计算机学院',
      height: 175,
      weight: 68.5,
      role: 'user',
      status: 'normal',
      freezeReasonCode: null,
      freezeReasonText: null,
      freezeTime: null,
      createTime: '2026-01-01 10:00:00',
      lastLoginTime: '2026-05-25 08:30:00',
    },
    {
      userId: 'usr_000002',
      phone: '139****1234',
      rawPhone: '13900131234',
      password: 'Aa123456',
      nickname: '用户_A1b2C3d4',
      avatar: '',
      gender: '女',
      grade: '大一',
      birthDate: '未设置',
      department: '软件学院',
      height: '未设置',
      weight: '未设置',
      role: 'user',
      status: 'frozen',
      freezeReasonCode: 'security_risk',
      freezeReasonText: '安全风险',
      freezeTime: '2026-05-20 19:12:00',
      createTime: '2026-02-05 13:20:00',
      lastLoginTime: '2026-05-20 19:12:00',
    },
  ],
  admins: [
    {
      userId: 'adm_001',
      phone: mockConfig.admin.phone,
      password: mockConfig.admin.password,
      nickname: '系统管理员',
      avatar: '',
      role: 'super_admin',
      status: 'normal',
    },
  ],
  semesters: [
    { id: 1, name: '2026-2027学年秋季学期', startDate: '2026-09-01', endDate: '2027-01-15' },
    { id: 2, name: '2025-2026学年春季学期', startDate: '2026-02-24', endDate: '2026-07-05' },
  ],
  events: [
    {
      id: 'evt_000001',
      title: '高等数学',
      category: 'course',
      startTime: '2026-05-27 08:00',
      endTime: '2026-05-27 09:40',
      location: '理学楼301',
      remark: '',
      reminder: '15min',
      status: 'pending',
    },
    {
      id: 'evt_000002',
      title: '软工大作业',
      category: 'ddl',
      deadline: '2026-05-27 23:59',
      reminder: '1day',
      status: 'pending',
      countdown: '今天',
    },
  ],
  courses: [
    {
      id: 'crs_000001',
      title: '高等数学',
      weekPattern: '1-16周',
      dayOfWeek: 1,
      startPeriod: 1,
      endPeriod: 2,
      location: '理学楼301',
    },
  ],
  announcements: [
    {
      id: 1,
      content: '系统将于今晚23:00进行维护，请提前保存数据。',
      type: 'system',
      dismissible: true,
      status: 'active',
      createTime: '2026-05-25 09:00:00',
    },
    {
      id: 2,
      content: '运动进度落后，明天下午14:00-16:00没课，去操场跑两圈吧？',
      type: 'sport',
      dismissible: true,
      status: 'offline',
      createTime: '2026-05-24 18:30:00',
    },
  ],
  sleepRecords: [
    {
      id: 'slp_000001',
      sleepDate: '2026-05-25',
      bedTime: '2026-05-24 23:30',
      wakeTime: '2026-05-25 07:15',
      duration: 7.75,
      quality: '较好',
      createTime: '2026-05-25 07:20:00',
    },
  ],
  exerciseRecords: [
    { id: 'exr_000001', type: '跑步', duration: 45, count: 1, recordDate: '2026-05-25' },
  ],
  weightRecords: [
    { id: 'wgt_000001', weight: 68.5, recordDate: '2026-05-25' },
  ],
  config: {
    health: {
      defaultExerciseTarget: 150,
    },
    api: {
      apiWeights: {
        courseHours: 30,
        examCount: 15,
        ddlCount: 5,
        completionRate: 20,
      },
    },
    system: {
      logRetentionDays: 90,
      abnormalCheckinThreshold: 50,
      procrastinationThreshold: 70,
    },
  },
  operationLogs: [
    {
      logId: 1,
      adminId: 'adm_001',
      adminNickname: '系统管理员',
      action: 'freeze_user',
      actionText: '冻结用户',
      targetType: 'user',
      targetId: 'usr_000002',
      targetNickname: '用户_A1b2C3d4',
      reasonCode: 'security_risk',
      reasonText: '安全风险',
      details: { phone: '139****1234' },
      ipAddress: '127.0.0.1',
      createTime: '2026-05-20 19:12:00',
    },
  ],
  exceptionLogs: [
    {
      logId: 1,
      userId: 'usr_000002',
      exceptionType: 'sql_injection',
      exceptionDetail: "检测到SQL注入尝试：' OR 1=1 --",
      requestUrl: '/schedule/events',
      ipAddress: '127.0.0.1',
      createTime: '2026-05-20 19:10:00',
    },
  ],
  metrics: makeDashboardMetrics(),
}

if (mockConfig.randomizeUsersOnStart) {
  db.users.push(...makeGeneratedUsers())
}

db.users.forEach((user) => {
  if (user.freezeReasonCode && !user.freezeReasonText) {
    user.freezeReasonText = FREEZE_REASON_TEXT[user.freezeReasonCode]
  }
})

// 统一响应格式，对齐接口文档中的 { code, message, data }。
function success(data = {}, message = '操作成功') {
  return { code: 200, message, data }
}

function error(code, message, subCode) {
  return { code, subCode, message, data: null }
}

function sendJson(res, payload, status = 200) {
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS',
  })
  res.end(JSON.stringify(payload))
}

// 解析 JSON 请求体。当前 Mock 主要服务前端联调，因此 multipart 文件上传只返回模拟结果。
function readBody(req) {
  return new Promise((resolve) => {
    let body = ''

    req.on('data', (chunk) => {
      body += chunk
    })
    req.on('end', () => {
      if (!body) {
        resolve({})
        return
      }

      try {
        resolve(JSON.parse(body))
      } catch {
        resolve({})
      }
    })
  })
}

// 对外返回用户信息时去掉密码和未脱敏手机号，避免管理端表格暴露敏感字段。
function publicUser(user) {
  const { password, rawPhone, ...rest } = user
  return rest
}

// 登录校验同时支持学生端 /auth/login 和管理端 /admin/login。
function loginUser(phone, password) {
  const admin = db.admins.find((item) => item.phone === phone && item.password === password)

  if (admin) {
    return {
      userId: admin.userId,
      nickname: admin.nickname,
      avatar: admin.avatar,
      token: 'mock-admin-token',
      refreshToken: 'mock-admin-refresh-token',
      expiresIn: 604800,
      role: admin.role,
      forceChangePassword: false,
    }
  }

  const user = db.users.find((item) => item.rawPhone === phone && item.password === password)

  if (!user) {
    return null
  }

  if (user.status === 'frozen') {
    return { frozen: true, reason: user.freezeReasonText || FREEZE_REASON_TEXT[user.freezeReasonCode] || '违规原因' }
  }

  return {
    userId: user.userId,
    nickname: user.nickname,
    avatar: user.avatar,
    token: 'mock-user-token',
    refreshToken: 'mock-user-refresh-token',
    expiresIn: 604800,
    role: user.role,
    forceChangePassword: Boolean(user.forceChangePassword),
  }
}

// 管理端用户列表支持关键词、状态和分页筛选，模拟真实后端分页接口。
function filterUsers(query) {
  const keyword = query.get('keyword')?.trim()
  const status = query.get('status')?.trim()
  const page = Number(query.get('page') || 1)
  const pageSize = Number(query.get('pageSize') || 20)
  let list = db.users.map(publicUser)

  if (keyword) {
    list = list.filter((item) => [item.userId, item.phone, item.nickname].some((value) => String(value).includes(keyword)))
  }

  if (status) {
    list = list.filter((item) => item.status === status)
  }

  const total = list.length
  const start = (page - 1) * pageSize

  return {
    total,
    page,
    pageSize,
    list: list.slice(start, start + pageSize),
  }
}

// 健康图表接口按 type 返回不同维度的近 7 天数据。
function chartData(type) {
  const xAxis = ['05-21', '05-22', '05-23', '05-24', '05-25', '05-26', '05-27']

  if (type === 'exercise') {
    return { xAxis, yAxis: [30, 0, 45, 20, 60, 0, 40], unit: '分钟' }
  }

  if (type === 'weight') {
    return { xAxis, yAxis: [68.8, null, 68.6, null, 68.5, null, 68.3], unit: 'kg' }
  }

  return { xAxis, yAxis: [7.5, 8, null, 7.2, 6.5, 8.5, 7.75], unit: '小时' }
}

// 主路由分发函数：根据 method + pathname 返回对应 Mock 数据。
async function route(req, res) {
  const url = new URL(req.url, `http://${req.headers.host}`)
  const path = url.pathname
  const method = req.method
  const body = await readBody(req)

  if (method === 'OPTIONS') {
    sendJson(res, {})
    return
  }

  if (method === 'POST' && path === '/auth/send_verify_code') {
    sendJson(res, success({ expireSeconds: 300 }, '验证码已发送，有效时间为5分钟'))
    return
  }

  if (method === 'POST' && path === '/auth/register') {
    const exists = db.users.some((item) => item.rawPhone === body.phone)

    if (exists) {
      sendJson(res, error(409, '该手机号已注册，请直接登录'), 409)
      return
    }

    const userId = `usr_${String(db.users.length + 1).padStart(6, '0')}`
    db.users.push({
      userId,
      phone: `${body.phone.slice(0, 3)}****${body.phone.slice(7)}`,
      rawPhone: body.phone,
      password: body.password,
      nickname: `用户_${Math.random().toString(36).slice(2, 10)}`,
      avatar: '',
      gender: '未知',
      grade: '大一',
      birthDate: '未设置',
      department: '未设置',
      height: '未设置',
      weight: '未设置',
      role: 'user',
      status: 'normal',
      freezeReasonCode: null,
      freezeReasonText: null,
      freezeTime: null,
      createTime: '2026-05-27 10:00:00',
      lastLoginTime: '2026-05-27 10:00:00',
    })
    sendJson(res, success({ userId, token: 'mock-user-token' }, '注册成功'))
    return
  }

  if (method === 'POST' && path === '/auth/refresh') {
    if (!body.refreshToken) {
      sendJson(res, error(401, 'refreshToken 无效'), 401)
      return
    }

    sendJson(res, success({
      token: `mock-token-${Date.now()}`,
      refreshToken: `mock-refresh-token-${Date.now()}`,
      expiresIn: 604800,
    }, '刷新成功'))
    return
  }

  if (method === 'POST' && (path === '/auth/login' || path === '/admin/login')) {
    const user = loginUser(body.phone, body.password)

    if (!user) {
      sendJson(res, error(400, '手机号或密码错误'), 400)
      return
    }

    if (user.frozen) {
      sendJson(res, error(403, `您的账号因[${user.reason}]已被冻结，请联系管理员`, 'account_frozen'), 403)
      return
    }

    if (path === '/admin/login' && user.role === 'user') {
      sendJson(res, error(403, '权限不足', 'insufficient_permission'), 403)
      return
    }

    sendJson(res, success(user, '登录成功'))
    return
  }

  if (method === 'GET' && path === '/user/profile') {
    sendJson(res, success(publicUser(db.users[0]), '获取成功'))
    return
  }

  if (method === 'PUT' && path === '/user/profile') {
    Object.assign(db.users[0], body)
    sendJson(res, success(publicUser(db.users[0]), '保存成功'))
    return
  }

  if (method === 'PUT' && path === '/user/password') {
    sendJson(res, success({}, '密码修改成功'))
    return
  }

  if (method === 'POST' && path === '/user/force_password') {
    sendJson(res, success({}, '密码修改成功'))
    return
  }

  if (method === 'POST' && path === '/user/avatar') {
    sendJson(res, success({ avatarUrl: 'https://example.com/mock-avatar.jpg' }, '上传成功'))
    return
  }

  if (method === 'PUT' && path === '/user/phone/bind') {
    sendJson(res, success({ forceLogout: true }, '换绑成功，请使用新手机号重新登录'))
    return
  }

  if (method === 'DELETE' && path === '/user/account') {
    sendJson(res, success({ forceLogout: true }, '账号及个人数据已成功注销并清除'))
    return
  }

  if (method === 'GET' && path === '/semester/list') {
    sendJson(res, success(db.semesters, '获取成功'))
    return
  }

  if (method === 'PUT' && path === '/semester/current') {
    sendJson(res, success({}, '设置成功'))
    return
  }

  if (method === 'GET' && (path === '/course/template' || path === '/schedule/template')) {
    res.writeHead(200, {
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'Access-Control-Allow-Origin': '*',
    })
    res.end('mock schedule template')
    return
  }

  if (method === 'POST' && (path === '/course/import' || path === '/schedule/import')) {
    sendJson(res, success({ successCount: 12, failCount: 0 }, '导入成功，共导入12门课程'))
    return
  }

  if (method === 'GET' && path === '/course/list') {
    sendJson(res, success(db.courses, '获取成功'))
    return
  }

  if (method === 'POST' && path === '/course') {
    const course = { id: `crs_${Date.now()}`, ...body }
    const event = {
      id: `evt_${Date.now()}`,
      title: body.title,
      category: 'course',
      weekPattern: body.weekPattern,
      dayOfWeek: body.dayOfWeek,
      startPeriod: body.startPeriod,
      endPeriod: body.endPeriod,
      location: body.location,
      reminder: '15min',
      status: 'pending',
    }
    db.courses.push(course)
    db.events.push(event)
    sendJson(res, success({ courseId: course.id, eventId: event.id }, '添加成功'))
    return
  }

  const courseMatch = path.match(/^\/course\/([^/]+)$/)
  if (courseMatch && method === 'PUT') {
    const course = db.courses.find((item) => item.id === courseMatch[1])
    Object.assign(course || {}, body)
    sendJson(res, success({}, '更新成功'))
    return
  }

  if (courseMatch && method === 'DELETE') {
    db.courses = db.courses.filter((item) => item.id !== courseMatch[1])
    sendJson(res, success({}, '删除成功'))
    return
  }

  if (method === 'GET' && path === '/schedule/events') {
    sendJson(res, success(db.events, '获取成功'))
    return
  }

  if (method === 'POST' && path === '/schedule/events') {
    const event = { id: `evt_${Date.now()}`, status: 'pending', ...body }
    db.events.push(event)
    sendJson(res, success({ id: event.id }, '创建成功'))
    return
  }

  const eventMatch = path.match(/^\/schedule\/events\/([^/]+)$/)
  if (eventMatch && method === 'PUT') {
    const event = db.events.find((item) => item.id === eventMatch[1])
    Object.assign(event || {}, body)
    sendJson(res, success({}, '更新成功'))
    return
  }

  if (eventMatch && method === 'DELETE') {
    db.events = db.events.filter((item) => item.id !== eventMatch[1])
    sendJson(res, success({}, '删除成功'))
    return
  }

  const eventStatusMatch = path.match(/^\/schedule\/events\/([^/]+)\/status$/)
  if (eventStatusMatch && method === 'PATCH') {
    const event = db.events.find((item) => item.id === eventStatusMatch[1])
    if (event) {
      event.status = body.status
    }
    sendJson(res, success({}, '状态更新成功'))
    return
  }

  if (method === 'POST' && (path === '/schedule/events/check_conflict' || path === '/schedule/events/check-conflict')) {
    sendJson(res, success({ hasConflict: false }, '无冲突'))
    return
  }

  if (method === 'GET' && path === '/announcements') {
    sendJson(res, success(db.announcements.filter((item) => item.status === 'active'), '获取成功'))
    return
  }

  const dismissAnnouncementMatch = path.match(/^\/announcements\/([^/]+)\/dismiss$/)
  if (dismissAnnouncementMatch && method === 'POST') {
    sendJson(res, success({}, '已关闭'))
    return
  }

  if (method === 'GET' && path === '/health/sleep') {
    sendJson(res, success(db.sleepRecords, '获取成功'))
    return
  }

  if (method === 'POST' && path === '/health/sleep') {
    const record = { id: `slp_${Date.now()}`, ...body, duration: 7.5, sleepDate: body.recordDate, createTime: '2026-05-27 10:00:00' }
    db.sleepRecords.push(record)
    sendJson(res, success(record, '创建成功'))
    return
  }

  const sleepMatch = path.match(/^\/health\/sleep\/([^/]+)$/)
  if (sleepMatch && method === 'DELETE') {
    db.sleepRecords = db.sleepRecords.filter((item) => item.id !== sleepMatch[1])
    sendJson(res, success({}, '删除成功'))
    return
  }

  if (method === 'GET' && path === '/health/exercise') {
    sendJson(res, success(db.exerciseRecords, '获取成功'))
    return
  }

  if (method === 'POST' && path === '/health/exercise') {
    const record = { id: `exr_${Date.now()}`, ...body }
    db.exerciseRecords.push(record)
    sendJson(res, success(record, '创建成功'))
    return
  }

  if (method === 'GET' && path === '/health/weight') {
    sendJson(res, success(db.weightRecords, '获取成功'))
    return
  }

  if (method === 'POST' && path === '/health/weight') {
    const record = { id: `wgt_${Date.now()}`, ...body }
    db.weightRecords.push(record)
    sendJson(res, success(record, '创建成功'))
    return
  }

  if (method === 'GET' && path === '/health/chart') {
    sendJson(res, success(chartData(url.searchParams.get('type')), '获取成功'))
    return
  }

  if (method === 'GET' && path === '/analysis/sleep') {
    sendJson(res, success({ alerts: [{ type: '作息不规律', suggestion: '近期主睡眠段入睡时间波动大，建议今晚23:30前入睡' }], summary: { avgDuration: 7.5, avgQuality: '较好' } }, '获取成功'))
    return
  }

  if (method === 'GET' && path === '/analysis/exercise') {
    sendJson(res, success({ defaultTarget: 150, adjustedTarget: 150, adjustReason: '当前身体数据和压力指数正常，沿用默认目标', actualMinutes: 75, progress: 50, status: 'behind', suggestions: ['运动进度落后，明天下午14:00-16:00没课，去操场跑两圈吧？'] }, '获取成功'))
    return
  }

  if (method === 'GET' && path === '/analysis/pressure') {
    sendJson(res, success({ apiScore: 65, level: 'medium', levelText: '适中', breakdown: { courseHours: 6, courseWeight: 30, examCount: 1, ddlCount: 3, completionRate: 0.6 } }, '获取成功'))
    return
  }

  if (method === 'GET' && path === '/analysis/procrastination') {
    sendJson(res, success({ weekCompletionRate: 42, onTimeRate: 85, alert: '极限赶工', suggestion: '本周有85%的DDL在截止日当天完成，建议提前规划' }, '获取成功'))
    return
  }

  if (method === 'GET' && path === '/report/weekly') {
    sendJson(res, success({ weekNumber: 21, startDate: '2026-05-19', endDate: '2026-05-25', canRegenerate: true, regenerateAvailableUntil: '2026-05-28', sections: { schedule: { totalEvents: 28, completed: 25, overdue: 1 }, sleep: { avgDuration: 7.5, avgQuality: '较好' }, exercise: { totalMinutes: 150, targetMet: true }, weight: { startWeight: 68.5, endWeight: 68.3 }, pressure: { avgScore: 55, trend: 'stable' } }, suggestions: ['本周运动目标已达成，继续保持！'] }, '获取成功'))
    return
  }

  if (method === 'GET' && path === '/report/weekly/history') {
    sendJson(res, success([{ id: 1, weekNumber: 21, startDate: '2026-05-19', endDate: '2026-05-25' }, { id: 2, weekNumber: 20, startDate: '2026-05-12', endDate: '2026-05-18' }], '获取成功'))
    return
  }

  if (method === 'POST' && path === '/report/weekly/regenerate') {
    sendJson(res, success({ regenerated: true }, '周报已重新生成'))
    return
  }

  const weeklyReportMatch = path.match(/^\/report\/weekly\/([^/]+)$/)
  if (weeklyReportMatch && method === 'GET') {
    sendJson(res, success({ weekNumber: 21, startDate: '2026-05-19', endDate: '2026-05-25', canRegenerate: true, regenerateAvailableUntil: '2026-05-28', sections: { schedule: { totalEvents: 28, completed: 25, overdue: 1 }, sleep: { avgDuration: 7.5, avgQuality: '较好' }, exercise: { totalMinutes: 150, targetMet: true }, weight: { startWeight: 68.5, endWeight: 68.3 }, pressure: { avgScore: 55, trend: 'stable' } }, suggestions: ['本周运动目标已达成，继续保持！'] }, '获取成功'))
    return
  }

  if (method === 'GET' && path === '/admin/dashboard') {
    // 可配置为每次请求生成新统计值，方便检查 ECharts 是否响应数据变化。
    if (mockConfig.randomizeDashboardOnRequest) {
      db.metrics = makeDashboardMetrics()
    }

    sendJson(res, success({
      totalUsers: db.users.length,
      ...db.metrics,
    }, '获取成功'))
    return
  }

  if (method === 'GET' && path === '/admin/users') {
    sendJson(res, success(filterUsers(url.searchParams), '获取成功'))
    return
  }

  if (method === 'POST' && path === '/admin/admins') {
    const adminId = `adm_${String(db.admins.length + 1).padStart(3, '0')}`
    db.admins.push({
      userId: adminId,
      phone: body.phone,
      password: body.password,
      nickname: body.nickname,
      avatar: '',
      role: 'admin',
      status: 'normal',
    })
    db.operationLogs.unshift({
      logId: Date.now(),
      adminId: 'adm_001',
      adminNickname: '系统管理员',
      action: 'create_admin',
      actionText: '创建管理员',
      targetType: 'user',
      targetId: adminId,
      targetNickname: body.nickname,
      reasonCode: '',
      reasonText: '',
      details: { phone: body.phone },
      ipAddress: '127.0.0.1',
      createTime: '2026-05-27 10:00:00',
    })
    sendJson(res, success({ userId: adminId, role: 'admin' }, '创建成功'))
    return
  }

  const freezeMatch = path.match(/^\/admin\/users\/([^/]+)\/freeze$/)
  if (freezeMatch && method === 'POST') {
    const user = db.users.find((item) => item.userId === freezeMatch[1])
    if (user) {
      user.status = 'frozen'
      user.freezeReasonCode = body.reasonCode
      user.freezeReasonText = FREEZE_REASON_TEXT[body.reasonCode] || '其他'
      user.freezeTime = '2026-05-27 14:30:00'
      db.operationLogs.unshift({
        logId: Date.now(),
        adminId: 'adm_001',
        adminNickname: '系统管理员',
        action: 'freeze_user',
        actionText: '冻结用户',
        targetType: 'user',
        targetId: user.userId,
        targetNickname: user.nickname,
        reasonCode: body.reasonCode,
        reasonText: user.freezeReasonText,
        details: { phone: user.phone },
        ipAddress: '127.0.0.1',
        createTime: user.freezeTime,
      })
    }
    sendJson(res, success({
      userId: user?.userId,
      freezeTime: user?.freezeTime,
      reasonCode: user?.freezeReasonCode,
      reasonText: user?.freezeReasonText,
    }, '冻结成功'))
    return
  }

  const unfreezeMatch = path.match(/^\/admin\/users\/([^/]+)\/unfreeze$/)
  if (unfreezeMatch && method === 'POST') {
    const user = db.users.find((item) => item.userId === unfreezeMatch[1])
    if (user) {
      user.status = 'normal'
      user.freezeReasonCode = null
      user.freezeReasonText = null
      user.freezeTime = null
      db.operationLogs.unshift({
        logId: Date.now(),
        adminId: 'adm_001',
        adminNickname: '系统管理员',
        action: 'unfreeze_user',
        actionText: '解封用户',
        targetType: 'user',
        targetId: user.userId,
        targetNickname: user.nickname,
        reasonCode: '',
        reasonText: '',
        details: { phone: user.phone },
        ipAddress: '127.0.0.1',
        createTime: '2026-05-27 15:00:00',
      })
    }
    sendJson(res, success({ userId: user?.userId, unfreezeTime: '2026-05-27 15:00:00' }, '解封成功'))
    return
  }

  const resetPasswordMatch = path.match(/^\/admin\/users\/([^/]+)\/reset_password$/)
  if (resetPasswordMatch && method === 'POST') {
    const user = db.users.find((item) => item.userId === resetPasswordMatch[1]) || db.admins.find((item) => item.userId === resetPasswordMatch[1])
    if (user) {
      user.password = body.tempPassword
      user.forceChangePassword = true
    }
    db.operationLogs.unshift({
      logId: Date.now(),
      adminId: 'adm_001',
      adminNickname: '系统管理员',
      action: 'reset_password',
      actionText: '重置密码',
      targetType: 'user',
      targetId: resetPasswordMatch[1],
      targetNickname: user?.nickname || resetPasswordMatch[1],
      reasonCode: '',
      reasonText: '',
      details: { forceChange: true },
      ipAddress: '127.0.0.1',
      createTime: '2026-05-27 15:30:00',
    })
    sendJson(res, success({ tempPassword: body.tempPassword, forceChange: true }, '密码重置成功'))
    return
  }

  if (method === 'GET' && path === '/admin/announcements') {
    const status = url.searchParams.get('status')
    const type = url.searchParams.get('type')
    const page = Number(url.searchParams.get('page') || 1)
    const pageSize = Number(url.searchParams.get('pageSize') || 20)
    let list = [...db.announcements]

    if (status) {
      list = list.filter((item) => item.status === status)
    }
    if (type) {
      list = list.filter((item) => item.type === type)
    }

    const total = list.length
    const start = (page - 1) * pageSize
    sendJson(res, success({ total, page, pageSize, list: list.slice(start, start + pageSize) }, '获取成功'))
    return
  }

  if (method === 'POST' && path === '/admin/announcements') {
    db.announcements.unshift({
      id: Date.now(),
      content: body.content,
      type: body.type,
      dismissible: true,
      status: 'active',
      createTime: '2026-05-27 10:00:00',
    })
    db.operationLogs.unshift({
      logId: Date.now(),
      adminId: 'adm_001',
      adminNickname: '系统管理员',
      action: 'publish_announcement',
      actionText: '发布公告',
      targetType: 'announcement',
      targetId: String(db.announcements[0].id),
      targetNickname: body.type,
      reasonCode: '',
      reasonText: '',
      details: { content: body.content },
      ipAddress: '127.0.0.1',
      createTime: '2026-05-27 10:00:00',
    })
    sendJson(res, success({ id: db.announcements[0].id }, '发布成功'))
    return
  }

  const offlineMatch = path.match(/^\/admin\/announcements\/([^/]+)\/offline$/)
  if (offlineMatch && method === 'PUT') {
    const announcement = db.announcements.find((item) => String(item.id) === offlineMatch[1])
    if (announcement) {
      announcement.status = 'offline'
    }
    db.operationLogs.unshift({
      logId: Date.now(),
      adminId: 'adm_001',
      adminNickname: '系统管理员',
      action: 'offline_announcement',
      actionText: '下架公告',
      targetType: 'announcement',
      targetId: offlineMatch[1],
      targetNickname: announcement?.type || '',
      reasonCode: '',
      reasonText: '',
      details: { content: announcement?.content },
      ipAddress: '127.0.0.1',
      createTime: '2026-05-27 11:00:00',
    })
    sendJson(res, success({}, '下架成功'))
    return
  }

  if (method === 'GET' && path === '/admin/config') {
    sendJson(res, success(db.config, '获取成功'))
    return
  }

  if (method === 'PUT' && path === '/admin/config') {
    db.config = {
      health: { ...db.config.health, ...body.health },
      api: { apiWeights: { ...db.config.api.apiWeights, ...body.api?.apiWeights } },
      system: { ...db.config.system, ...body.system },
    }
    db.operationLogs.unshift({
      logId: Date.now(),
      adminId: 'adm_001',
      adminNickname: '系统管理员',
      action: 'update_config',
      actionText: '更新配置',
      targetType: 'config',
      targetId: 'system',
      targetNickname: '系统配置',
      reasonCode: '',
      reasonText: '',
      details: db.config,
      ipAddress: '127.0.0.1',
      createTime: '2026-05-27 16:00:00',
    })
    sendJson(res, success({}, '配置更新成功'))
    return
  }

  if (method === 'GET' && path === '/admin/logs/operation') {
    const page = Number(url.searchParams.get('page') || 1)
    const pageSize = Number(url.searchParams.get('pageSize') || 20)
    const action = url.searchParams.get('action')
    const targetType = url.searchParams.get('targetType')
    let list = [...db.operationLogs]

    if (action) {
      list = list.filter((item) => item.action === action)
    }
    if (targetType) {
      list = list.filter((item) => item.targetType === targetType)
    }

    const total = list.length
    const start = (page - 1) * pageSize
    sendJson(res, success({ total, page, pageSize, list: list.slice(start, start + pageSize) }, '获取成功'))
    return
  }

  if (method === 'GET' && path === '/admin/logs/exception') {
    const page = Number(url.searchParams.get('page') || 1)
    const pageSize = Number(url.searchParams.get('pageSize') || 20)
    const exceptionType = url.searchParams.get('exceptionType')
    let list = [...db.exceptionLogs]

    if (exceptionType) {
      list = list.filter((item) => item.exceptionType === exceptionType)
    }

    const total = list.length
    const start = (page - 1) * pageSize
    sendJson(res, success({ total, page, pageSize, list: list.slice(start, start + pageSize) }, '获取成功'))
    return
  }

  if (method === 'DELETE' && path === '/admin/logs/clean') {
    const logType = url.searchParams.get('logType') || 'all'
    let deletedCount = 0

    if (logType === 'operation' || logType === 'all') {
      deletedCount += db.operationLogs.length
      db.operationLogs = []
    }
    if (logType === 'exception' || logType === 'all') {
      deletedCount += db.exceptionLogs.length
      db.exceptionLogs = []
    }

    sendJson(res, success({ deletedCount }, '日志清理成功'))
    return
  }

  sendJson(res, error(404, `Mock接口不存在：${method} ${path}`), 404)
}

const server = http.createServer((req, res) => {
  route(req, res).catch((err) => {
    console.error(err)
    sendJson(res, error(500, 'Mock服务内部错误'), 500)
  })
})

server.listen(PORT, () => {
  console.log(`Mock API server is running at http://localhost:${PORT}`)
  console.log(`Mock admin account: ${mockConfig.admin.phone} / ${mockConfig.admin.password}`)
})
