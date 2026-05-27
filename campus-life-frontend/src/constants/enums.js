export const USER_STATUS_TEXT = {
  normal: '正常',
  frozen: '已冻结',
}

export const ANNOUNCEMENT_TYPE_TEXT = {
  system: '系统公告',
  sport: '运动提醒',
}

export const ANNOUNCEMENT_STATUS_TEXT = {
  online: '展示中',
  offline: '已下架',
}

export const FREEZE_REASONS = [
  { label: '内容违规（昵称/备注）', value: '内容违规' },
  { label: '安全异常（SQL注入/脚本尝试）', value: '安全异常' },
  { label: '恶意刷单/异常打卡', value: '恶意刷单打卡' },
  { label: '其他原因', value: '其他原因' },
]

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
