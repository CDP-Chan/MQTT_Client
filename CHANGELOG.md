# 更新日志 / Changelog

本项目所有值得记录的变更都会汇总在此文件中。
All notable changes to this project are documented in this file.

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。
Based on [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/lang/zh-CN/).

## [V1.0.2-Patch] - 2026-08-09

### 修复 / Fixes

- 修复了某些机型不支持外部存储写入与读取的bug，数据存储地址改为应用内部 data（`filesDir/Mqtt`），不再依赖外部存储；升级安装时会自动把旧的外部存储数据一次性迁移到内部存储。

  Fixed the bug where some device models do not support external storage read/write. Data is now stored in the app's internal data directory (`filesDir/Mqtt`) and no longer depends on external storage; on upgrade, data from the old external location is migrated into internal storage automatically.
- 以后将不会发布依赖外部存储的版本。

  Future releases will no longer depend on external storage.

### 变更 / Changes

- 更改字体/颜色后仅刷新当前界面，不再重启应用，不会删除任何数据。

  Changing font or colors now only refreshes the current screen instead of restarting the app; no data is deleted.
- 客户端删除功能全部保留（删除主题、删除消息、清空日志）；服务端不主动清空数据，仅在“恢复出厂设置”时执行完整恢复。

  All client-side delete features are retained (delete topic, delete messages, clear log); the service never proactively clears data — a full reset happens only via “Factory Reset”.

## [1.0.2] - 2026-08-08

### 界面优化 / UI Improvements

- 所有文字与边框颜色可调：新增“注释颜色”，边框、文字、注释三套颜色可分别调整；弹窗、Snackbar、列表项等统一应用自定义颜色；默认注释颜色为白色。

  All text and border colors are now customizable: a “comment color” setting was added, and border, text, and comment colors can be adjusted separately. Dialogs, snackbars, list items, etc. consistently use the custom colors, and the default comment color is white.
- 添加主题页面的输入框提示文字（占位符）固定为灰色，不受颜色设置影响。

  Placeholder text on the Add Topic screen is fixed to gray and unaffected by color settings.
- 系统设置页面文字放大：标题 22sp、选项名称 18sp、备注 15sp、按钮 16sp。

  Enlarged text on the System Settings screen: title 22sp, option labels 18sp, notes 15sp, button text 16sp.
- 帮助文档重新排版：注释段落（开头、FAQ 标题、结尾）颜色可调；FAQ 问答正文锁定纯白，不受颜色设置影响。

  Restructured the help document: comment sections (intro, FAQ title, closing paragraphs) are color-adjustable, while the FAQ Q&A body is locked to pure white.
- 主题卡片边框与删除图标跟随自定义边框颜色。

  Topic card borders and the delete icon follow the custom border color.

### 功能修复 / Bug Fixes

- 添加/编辑主题前检查存储写入权限：无权限时先申请（API 30+ “所有文件访问”，API 26-29 WRITE_EXTERNAL_STORAGE），拒绝则拒绝保存并提示，不再静默失败。

  Added a storage write permission check before saving a topic: it requests permission when missing (All files access on API 30+, WRITE_EXTERNAL_STORAGE on API 26-29) and refuses saving with a prompt when denied.
- 修复无主题时点击“连接”无反应的问题，改为提示“无主题已添加 / No topics added yet”，样式与颜色受用户设置控制。

  Fixed the Connect button doing nothing when no topics exist: it now shows “No topics added yet” with the same style and user-controlled colors.
- 添加主题后服务自动重连，删除主题后旧连接断开；服务日志统一写入 `filesDir/debug_log.txt`。

  The service now auto-reconnects after adding a topic and closes old connections after deleting one; service logs are uniformly written to `filesDir/debug_log.txt`.
- 首次启动语言设置生效：未授予存储权限时先缓存在内部 data，授权后自动同步。

  First-launch language setting now works: it is cached internally until storage permission is granted, then synced automatically.
- 任务移除后服务可靠重启（前台服务 + 精确闹钟，无权限时回退普通闹钟）；WakeLock 定时续期、销毁时统一释放。

  The service now restarts reliably after task removal (foreground service + exact alarm, falling back to a regular alarm without permission); the WakeLock is renewed periodically and released on destroy.
- 包名更改为 `com.CDP.mqtt_client`（安装身份变更，升级前请先卸载旧版本）。

  Package renamed to `com.CDP.mqtt_client` (install identity changed; please uninstall the old version before upgrading).

### 说明 / Notes

- “发送消息”开关关闭后，新消息弹窗通知静默，但前台服务常驻通知保留（Android 前台服务强制要求）。

  With the “Send Messages” switch off, new message pop-up notifications are silenced, but the foreground service notification remains (required by Android).
- 精确闹钟在 Android 14+ 上默认可能被系统拒绝，此时自动回退为普通闹钟。

  Exact alarms may be denied by default on Android 14+, in which case the app falls back to a regular alarm.

## [1.0.1] - 2026-07-29

V1.0.0 至 V1.0.1 更新日志 / V1.0.0 To V1.0.1 Update_Log

### 内容更新 / Content Updates

- 设置页面重新分区为“基础设置”、“个性化设置”、“系统设置”。

  Settings reorganized into “Basic Settings”, “Personalization”, and “System Settings”.
- 新增个性化设置：自定义边框颜色与文字颜色（RGB 调色盘）。

  Added personalization: customizable border and text colors (RGB sliders).
- 帮助文档新增 FAQ 内容，涵盖常见使用问题。

  Added FAQ section in Help, covering common usage questions.

### 技术更新 / Technical Updates

- 设置与数据存储至 /Mqtt 公共目录，便于备份；卸载应用或清除数据仍会删除。

  Settings and data stored in the /Mqtt public directory for easy backup; uninstalling or clearing app data will still remove them.
- 修复多项稳定性问题，优化消息列表刷新性能。

  Fixed several stability issues and optimized message list refresh performance.

### 更新计划 / Future Plans

- 持续增加更细粒度的设置选项与功能开关。

  Add more granular settings options and feature toggles.
- 以 Kotlin MQTT 库替换 Eclipse Paho，新增 WS/WSS 协议支持。

  Replace Eclipse Paho with a Kotlin MQTT library, adding WS/WSS protocol support.

## [1.0.0]

- 初始发布版本。

  Initial release.
