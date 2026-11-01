# AGENTS — ATask

面向 agent 的本仓库补充约定。工程结构、构建命令、架构见 `CLAUDE.md`（沿用上游 Vicu 文档，
与 `docs/ios.md`）；本文只记录**发布与环境**的增量，冲突时以本文为准。

## TestFlight 发布（免 Xcode 登录）

`iosApp/scripts/testflight.sh` 上传**优先走 App Store Connect API key**，不依赖 Xcode 里
Apple 账号的登录态（该 session 会不定期失效，失效时 export 报
`App Store Connect access ... is required` 并要求重新登录，GUI Organizer 同样传不了）。

- 凭据来源：`~/Run/Env/apple/appstore-connect.env`（脚本自动 source；导出
  `ASC_KEY_PATH` / `ASC_KEY_ID` / `ASC_ISSUER_ID` 三项即启用，也可预先 export 覆盖）。
  私钥等价副本在 `~/.appstoreconnect/private_keys/` 与 `~/Run/Env/apple/private_keys/`。
- env 不存在时回退 Xcode 账号签名（`Configuration/Local.xcconfig` 的 `DEVELOPMENT_TEAM`，
  需 Settings → Apple Accounts 已登录）。
- 构建号 = `versionCode × 100 + BUILD_ATTEMPT`（`iosApp/scripts/generate.sh`）。同一
  version 重复上传递增 `BUILD_ATTEMPT`：`BUILD_ATTEMPT=1 iosApp/scripts/testflight.sh`，
  不要为此改 `version.properties` 的 `versionCode`（那是 Android 共用的）。
- 上传成功后 App Store Connect 还要处理几分钟到几十分钟，TestFlight 列表才出现新构建。

## 常用路径

| 项 | 值 |
|---|---|
| Team | `C6422MSGP6`（`ASC_TEAM_ID`） |
| App ID | `6826964058`（`ASC_APP_ID`） |
| 模拟器 UI 自动化 | 本机无 Simulator.app（Xcode 27 仅 Device Hub，AX 遥控点击不可靠）；驱动 springboard 用独立 XCUITest 工程（参考 Flow 仓库 `261031-vicu-ios改造/01-widget行数修正.md`） |
| 模拟器 widget 调试 | 快照 JSON 在 App Group `group.com.yes365.atask.debug` 的 `widgets/` 下（catalog.json + views/*.json），未登录时可直接注入测试数据 |
