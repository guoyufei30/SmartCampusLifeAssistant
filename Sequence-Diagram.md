# 智慧校园生活助手 - 序列图

## 使用方法

### 方法一：在线预览
1. 访问 [Mermaid Live Editor](https://mermaid.live)
2. 复制粘贴下方代码块
3. 点击 PNG/SVG 下载

### 方法二：命令行生成
```bash
npm install -g @mermaid-js/mermaid-cli
mmdc -i sequence-diagram.mmd -o sequence-diagram.png -b transparent -w 2400
```

---

## 序列图 1：统一登录流程

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant F as 前端
    participant A as API网关
    participant S as 认证服务
    participant UDB as 用户库
    participant C as 系统配置库

    U->>F: 输入手机号+密码
    F->>A: POST /auth/login {phone, password}
    A->>S: 转发登录请求
    S->>UDB: 查询用户信息
    UDB-->>S: 返回用户记录

    alt 账号状态正常
        S->>S: 生成JWT Token
        S->>C: 查询系统配置
        C-->>S: 返回配置
        S-->>A: {code: 200, data: {userId, role, token}}
        A-->>F: 返回登录成功
        F->>F: 根据role跳转

        alt role = user
            F->>F: 跳转用户端首页
        else role = admin or super_admin
            F->>F: 跳转管理后台
        end

    else 账号已冻结
        S-->>A: {code: 403, message: "账号已被冻结"}
        A-->>F: 返回403
        F->>F: 弹窗提示"请联系管理员"
    end
```

---

## 序列图 2：管理员冻结用户

```mermaid
sequenceDiagram
    autonumber
    participant A as 管理员
    participant F as 前端(管理后台)
    participant S as API服务
    participant UDB as 用户库
    participant LOG as 日志库

    A->>F: 选择用户，点击"冻结"
    F->>F: 弹出冻结原因选择框
    A->>F: 选择原因(枚举选择)

    F->>S: POST /admin/users/{userId}/freeze<br/>{reasonCode: "content_violation"}
    S->>UDB: 开启事务
    S->>UDB: UPDATE user SET status='frozen'<br/>frozen_time=NOW()<br/>frozen_reason='content_violation'
    UDB-->>S: 更新成功

    S->>LOG: INSERT operation_log
    LOG-->>S: 记录成功

    S->>UDB: 提交事务
    UDB-->>S: 提交成功
    S-->>F: {code: 200, data: {freezeTime, reasonCode, reasonText}}
    F->>A: 展示"冻结成功"提示
    F->>F: 刷新用户列表
```

---

## 序列图 3：睡眠记录（含异常确认）

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant F as 前端
    participant S as API服务
    participant C as 配置库
    participant DB as 健康数据库

    U->>F: 填写睡眠数据<br/>(入睡23:30, 起床00:00)
    F->>S: POST /health/sleep<br/>{bedTime, wakeTime, quality}

    S->>S: 计算睡眠时长: 0.5小时
    S->>S: 检测时长异常?

    alt 时长异常 (< 1小时 或 > 18小时)
        S-->>F: {code: 200, data: {requiresConfirm: true, duration: 0.5}}
        F->>F: 弹窗"睡眠时长异常，确认提交?"
        U->>F: 点击"确认提交"

        F->>S: POST /health/sleep<br/>{..., confirmAbnormal: true}
        S->>DB: INSERT sleep_record<br/>{isAbnormal: true}
        DB-->>S: 记录成功
        S-->>F: {code: 200, data: {id, isAbnormal: true}}

    else 时长正常
        S->>DB: INSERT sleep_record
        DB-->>S: 记录成功
        S-->>F: {code: 200, data: {id, duration}}
    end

    F-->>U: 展示"记录成功"
```

---

## 序列图 4：异常打卡自动冻结

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户(恶意)
    participant F as 前端
    participant S as API服务
    participant C as 配置库
    participant UDB as 用户库
    participant LOG as 日志库

    loop 快速连续提交
        F->>S: POST /health/sleep {data}
        S->>C: 查询abnormalCheckinThreshold
        C-->>S: 返回50次/分钟
        S->>S: 计数器+1
    end

    alt 1分钟内超过阈值
        S->>UDB: UPDATE user SET status='frozen'<br/>frozen_reason='abnormal_checkin'
        UDB-->>S: 冻结成功

        S->>LOG: INSERT operation_log<br/>{action: 'auto_freeze_user'}
        LOG-->>S: 记录成功

        S->>LOG: INSERT exception_log<br/>{exceptionType: 'abnormal_checkin', exceptionDetail}
        LOG-->>S: 记录成功

        S-->>F: {code: 403, message: "账号因异常打卡已被冻结"}
        F-->>U: 弹窗提示冻结

    else 未超过阈值
        S-->>F: {code: 200}
        F-->>U: 记录成功
    end
```

---

## 序列图 5：创建管理员（仅超级管理员）

```mermaid
sequenceDiagram
    autonumber
    participant SA as 超级管理员
    participant F as 前端(管理后台)
    participant S as API服务
    participant V as 短信服务
    participant UDB as 用户库
    participant LOG as 日志库

    SA->>F: 点击"创建管理员"
    F->>SA: 输入手机号、密码、昵称
    SA->>F: 点击发送验证码
    F->>S: POST /auth/send_verifyCode<br/>{phone, type: "bind"}
    S->>V: 发送短信验证码
    V-->>S: 发送成功
    S-->>F: {code: 200}
    F-->>SA: 展示"验证码已发送"

    SA->>F: 输入验证码，点击创建
    F->>S: POST /admin/admins<br/>{phone, password, nickname, verifyCode}
    S->>S: 验证验证码

    S->>UDB: INSERT user<br/>{role: 'admin'}
    UDB-->>S: 创建成功

    S->>LOG: INSERT operation_log<br/>{action: 'create_admin'}
    LOG-->>S: 记录成功

    S-->>F: {code: 200, data: {userId, role: 'admin'}}
    F-->>SA: 展示"创建成功"
```

---

## 序列图 6：周报生成

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant F as 前端
    participant S as API服务
    participant T as 定时任务
    participant DB as 数据库

    rect rgb(240, 248, 255)
        Note over T,U: 自动生成（每周一凌晨）
        T->>S: 触发周报生成
        S->>DB: 查询上周日程完成情况
        DB-->>S: 返回数据
        S->>DB: 查询上周睡眠记录
        DB-->>S: 返回数据
        S->>DB: 查询上周运动记录
        DB-->>S: 返回数据
        S->>DB: 查询体重变化
        DB-->>S: 返回数据
        S->>S: 计算学业压力指数
        S->>S: 生成建议
        S->>DB: INSERT weekly_report
        DB-->>S: 保存成功
    end

    rect rgb(255, 250, 240)
        Note over U,F: 用户手动获取
        U->>F: 进入周报页面
        F->>S: GET /report/weekly
        S->>DB: 查询本周周报
        DB-->>S: 返回周报数据
        S-->>F: {data: WeeklyReport}
        F-->>U: 展示周报卡片
    end

    alt 周报需要重新生成
        U->>F: 点击"重新生成"
        F->>S: POST /report/weekly/regenerate
        S->>S: 重新计算各模块数据
        S->>DB: UPDATE weekly_report
        DB-->>S: 更新成功
        S-->>F: 返回新周报
        F-->>U: 展示更新后的周报
    end
```

---

*文档版本：V1.2*
*最后更新：2026-05-31*
