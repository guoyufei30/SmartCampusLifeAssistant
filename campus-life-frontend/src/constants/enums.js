export const USER_STATUS_TEXT = {
  normal: '正常',
  frozen: '已冻结',
}

export const ANNOUNCEMENT_TYPE_TEXT = {
  system: '系统公告',
  sport: '运动提醒',
}

export const ANNOUNCEMENT_STATUS_TEXT = {
  active: '展示中',
  offline: '已下架',
}

export const FREEZE_REASONS = [
  { label: '内容违规', value: 'content_violation' },
  { label: '安全风险', value: 'security_risk' },
  { label: '异常打卡', value: 'abnormal_checkin' },
  { label: '其他', value: 'other' },
]

export const FREEZE_REASON_TEXT = {
  content_violation: '内容违规',
  security_risk: '安全风险',
  abnormal_checkin: '异常打卡',
  other: '其他',
}

export const LOG_ACTION_TEXT = {
  freeze_user: '冻结用户',
  unfreeze_user: '解封用户',
  auto_freeze_user: '系统自动冻结用户',
  reset_password: '重置密码',
  publish_announcement: '发布公告',
  offline_announcement: '下架公告',
  update_config: '更新配置',
  create_admin: '创建管理员',
  clean_logs: '清理日志',
}

export const EVENT_CATEGORY_TEXT = {
  course: '课程',
  exam: '考试',
  activity: '活动',
  ddl: '作业/DDL',
  personal: '个人事项',
}

export const EVENT_STATUS_TEXT = {
  pending: '待办中',
  in_progress: '进行中',
  overdue: '已逾期',
  completed: '已完成',
}

export const REMINDER_TEXT = {
  none: '不提醒',
  '15min': '提前15分钟',
  '1hour': '提前1小时',
  '1day': '提前1天',
}

export const SLEEP_QUALITY_OPTIONS = ['极佳', '较好', '一般', '较差']
