# ATask

A self-hosted task manager for iOS and Android, sharing one Kotlin / Compose Multiplatform codebase, backed by [Vikunja](https://vikunja.io/).

ATask is a fork of [rendyhd/Vicu-Mobile](https://github.com/rendyhd/Vicu-Mobile), focused on two things: **calendar** and **internationalization**.

## What this fork changes

| Area | Change |
|---|---|
| Localization | Full Simplified Chinese UI; follows the system language (`en` / `zh-Hans`); localized date phrasing and display name |
| Chinese input parsing | Natural-language date and repetition recognition — `明天下午三点`, `下周三`, `每周一`. `extractDate` dispatches on CJK, the English path is unchanged, and the Chinese phrase stays in the task title |
| Calendar | The Upcoming tab became a calendar: month grid plus the day's events and tasks, horizontal swipe to change month, and a Today button |
| Device calendar | Read-only EventKit sync, with a setting for which device calendars to show |
| Appearance | Dark-mode app icon; new habit and project forms open as full-width bottom sheets |

## Relationship to upstream

- Upstream: [`rendyhd/Vicu-Mobile`](https://github.com/rendyhd/Vicu-Mobile), MIT
- The cross-app contract (`docs/cross-app-semantics-v1.md` and friends) and the hidden-task marker format are kept as they are, so ATask can still share a Vikunja server with Vicu desktop and other Vicu clients.
- The app identifier is `com.yes365.atask`. Internal data-format markers (`vicu-prefs`, `vicu-routine`, the database name, ...) are deliberately unchanged, so data stays interchangeable with upstream clients.

## Building

Requires JDK 17+, the Android SDK for the Android app, and Xcode 26+ with XcodeGen for iOS.

```bash
# Android
./gradlew assembleDebug

# iOS
iosApp/scripts/generate.sh                       # writes iosApp/ATask.xcodeproj
xcodebuild -project iosApp/ATask.xcodeproj -scheme ATask \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

iOS device builds need `DEVELOPMENT_TEAM` in `iosApp/Configuration/Local.xcconfig`, which is not checked in.

## License

MIT, see [LICENSE](LICENSE). Upstream copyright belongs to [rendyhd](https://github.com/rendyhd).

---

# ATask 中文说明

一个自托管的任务管理客户端，iOS 与 Android 共用一套 Kotlin / Compose Multiplatform 代码，后端为 [Vikunja](https://vikunja.io/)。

本项目基于 [rendyhd/Vicu-Mobile](https://github.com/rendyhd/Vicu-Mobile) 改造，聚焦两件事：**日历** 与 **国际化**。

## 本分支做了什么

| 方向 | 改动 |
|---|---|
| 国际化 | 界面全量简体中文；跟随系统语言自动切换（`en` / `zh-Hans`）；日期措辞与应用显示名本地化 |
| 中文输入解析 | 中文自然语言日期与重复规则识别：`明天下午三点`、`下周三`、`每周一`。`extractDate` 按 CJK 分派，英文路径不变；中文短语保留在任务标题中 |
| 日历 | 原 Upcoming 页替换为日历视图：月网格 + 当日日程与任务，左右滑动切换月份，「今天」一键回到当前日期 |
| 系统日历 | 接入 EventKit 只读同步；可选择显示哪些设备日历 |
| 外观 | 新增深色模式应用图标；习惯与项目的新建表单改为底部弹层、通栏宽度 |

## 与上游的关系

- 上游：`rendyhd/Vicu-Mobile`，MIT 许可
- 保留上游的跨端契约（`docs/cross-app-semantics-v1.md` 等）与隐藏任务标记格式，因此仍可与 Vicu 桌面版共用同一个 Vikunja 服务端
- 应用标识已改为 `com.yes365.atask`；内部数据格式标识（`vicu-prefs`、`vicu-routine`、数据库名等）保持不变，以维持与上游客户端的数据互通

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

iOS 真机构建需要在 `iosApp/Configuration/Local.xcconfig` 中提供 `DEVELOPMENT_TEAM`，该文件不入库。

## 许可

MIT，见 [LICENSE](LICENSE)。上游版权归 [rendyhd](https://github.com/rendyhd)。
