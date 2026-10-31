# ATask

一个自托管的任务管理客户端，iOS 与 Android 共用一套 Kotlin / Compose Multiplatform 代码，后端是 [Vikunja](https://vikunja.io/)。

本项目基于 [rendyhd/Vicu-Mobile](https://github.com/rendyhd/Vicu-Mobile) 改造，聚焦两件事：**日历** 与 **国际化**。

## 本分支做了什么

| 方向 | 改动 |
|---|---|
| 国际化 | 界面全量简体中文；跟随系统语言自动切换（`en` / `zh-Hans`）；日期措辞本地化；应用显示名随语言变化 |
| 中文输入解析 | 新增中文自然语言识别：`明天下午三点`、`下周三`、`每周一` 等；`extractDate` 按 CJK 分派，英文路径不变；中文短语保留在任务标题中 |
| 日历 | 原 Upcoming 页替换为日历视图：月网格 + 当日日程/任务，左右滑动切换月份，「今天」一键回到当前日期 |
| 系统日历 | 接入 EventKit 只读同步；可选择显示哪些设备日历 |
| 外观 | 新增深色模式应用图标；习惯/项目的新建表单改为底部弹层、通栏宽度 |

## 与上游的关系

- 上游：`rendyhd/Vicu-Mobile`，MIT 许可
- 本项目保留上游的跨端契约（`docs/cross-app-semantics-v1.md` 等）与隐藏任务标记格式，因此仍可与 Vicu 桌面版共用同一个 Vikunja 服务端
- 应用标识已改为 `com.yes365.atask`；内部数据格式标识（`vicu-prefs`、`vicu-routine`、数据库名等）保持不变，以免破坏与上游客户端的数据互通

## 构建

前置：JDK 17+、Android SDK（仅 Android）、Xcode 26+ 与 XcodeGen（仅 iOS）。

```bash
# Android
./gradlew assembleDebug

# iOS
iosApp/scripts/generate.sh                       # 生成 iosApp/ATask.xcodeproj
xcodebuild -project iosApp/ATask.xcodeproj -scheme ATask \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

iOS 签名需要在 `iosApp/Configuration/Local.xcconfig` 中提供 `DEVELOPMENT_TEAM`（该文件不入库）。

## 许可

MIT，见 [LICENSE](LICENSE)。上游版权归 [rendyhd](https://github.com/rendyhd)。
