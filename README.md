# ATask

A self-hosted task manager for iOS and Android, sharing one Kotlin / Compose Multiplatform codebase, backed by [Vikunja](https://vikunja.io/).

ATask is a fork of [rendyhd/Vicu-Mobile](https://github.com/rendyhd/Vicu-Mobile), focused on two things: **calendar** and **internationalization**.

**中文说明见 [README_CN.md](README_CN.md).**

## What this fork changes

| Area | Change |
|---|---|
| Localization | Full Simplified Chinese UI; follows the system language (`en` / `zh-Hans`); localized date phrasing and display name |
| Chinese input parsing | Natural-language date and repetition recognition — `明天下午三点`, `下周三`, `每周一`. `extractDate` dispatches on CJK, the English path is unchanged, and the Chinese phrase stays in the task title |
| Calendar | A new Calendar view next to Upcoming: month grid plus the day's events and tasks, horizontal swipe to change month, and a Today button |
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
