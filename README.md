<div align="center">

<img src="https://raw.githubusercontent.com/slayercjc/GoGoGo/android16-baidu/docs/huanwei-icon.svg" width="112" alt="幻位图标" />

# 幻位 · HuanWei

**虚拟定位 · 自由探索**

蓝紫色原生 Android 定位工具，由 **Slayercjc** 维护，基于 [ZCShou/GoGoGo](https://github.com/ZCShou/GoGoGo) v1.12.2 修改。

[下载安装](https://github.com/slayercjc/GoGoGo/releases) · [问题反馈](https://github.com/slayercjc/GoGoGo/issues/new/choose) · [源码分支](https://github.com/slayercjc/GoGoGo/tree/android16-baidu) · [开发者](https://github.com/slayercjc)

</div>

## 当前开发版本

**幻位 1.1.2** · Android 16 适配分支：`android16-baidu`。

本仓库的默认分支 master 保留上游代码；幻位的功能修改、构建工作流和文档位于 **android16-baidu**。下载、构建和提交改动时请选对分支。README 同步展示在仓库首页，不代表 master 的源码已经替换。

本版修复：Android 16 / HyperOS 设置页顶部卡片被标题栏遮挡的问题。保留整条搜索栏点击搜索、卡片式设置和首次进入后直达地图。

## 功能

- 百度地图地点搜索、地图选点、坐标输入和普通／卫星地图切换。
- Android 模拟位置服务，以及悬浮摇杆移动。
- 定位历史与搜索历史。
- 与应用图标配套的蓝紫色界面、悬浮工具栏、可收起的定位卡片和独立停止按钮。
- 跟随系统的浅色／深色界面；地图底图仍由百度 SDK 提供。
- 从本仓库 Releases 检查更新，可选择仅 Wi-Fi 自动下载，校验后调用系统安装器。
- 应用内问题反馈直达本项目 Issues。

## 下载与使用

1. 在 [Releases](https://github.com/slayercjc/GoGoGo/releases) 下载正式签名 APK；如尚未发布，请在 [Actions](https://github.com/slayercjc/GoGoGo/actions/workflows/android16.yml) 成功的签名构建中下载 `HuanWei-signed` 并解压。
2. 安装后按界面提示授予定位、通知和悬浮窗权限。
3. 打开手机开发者选项，在“选择模拟位置信息应用”中选择 **幻位**。
4. 在地图上选点或搜索地点，点击“开始定位”；运行时可选新位置传送，点击“停止”结束。

覆盖升级请使用同一签名的安装包，无需先卸载。幻位 1.0 尚无应用内下载更新功能，需要手动安装一次 1.1。系统仍可能要求确认安装，应用不承诺静默安装。

## 兼容情况

| 项目 | 当前情况 |
| --- | --- |
| 最低系统 | Android 8.1（API 27） |
| 编译／目标 API | Android 16（API 36） |
| 安装包架构 | arm64-v8a |
| 已有使用反馈 | 小米 15 Pro / HyperOS 3.0.309.0，前一签名版用户反馈基本可用 |
| 新界面与更新流程 | 以 Actions 检查和后续真机测试结果为准 |

不同手机系统和其他应用对模拟位置的处理可能不同。不能保证所有应用采用模拟位置，也不保证所有 Android 16 设备均可用。ZIP 对齐检查不等于完整的 16 KB 原生库兼容认证。

## 自己编译与百度地图接入

使用 JDK 17、Gradle Wrapper 和 Android SDK 36。地图使用百度 Android SDK，需要申请自己的 AK，绑定 **包名 + APK 签名证书 SHA-1**。

- 包名：`com.zcshou.gogogo`（保持与现有安装和百度 AK 一致）。
- AK 通过 `BAIDU_MAP_AK` 环境变量或 Gradle 属性注入，不写死在源码中。
- GitHub Actions 签名构建所需 Secrets：`BAIDU_MAP_AK`、`ANDROID_KEYSTORE_BASE64`、`ANDROID_STORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。
- Debug 与 Release 证书不同；只为 Release 证书配置的 AK 不一定能让本地 Debug 地图正常显示。

详细步骤：[百度地图申请、签名与 GitHub 云编译教程](https://github.com/slayercjc/GoGoGo/blob/android16-baidu/docs/BAIDU_GITHUB_GUIDE.zh-CN.md)。

```bash
git clone --branch android16-baidu https://github.com/slayercjc/GoGoGo.git
cd GoGoGo
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## 发布与自动更新

发布新版本时递增 `versionCode`，设置新的 `versionName`，沿用原有签名。构建会从实际 APK 自动生成 `update.json`，包含版本、文件长度与 SHA-256。把它与 APK 一起上传到最新正式 Release，应用才能识别和下载。

应用下载后还会校验包名、版本和签名。下载由 Android 系统管理；无网络或 GitHub 暂不可达时可稍后重试。

完整操作：[发布新版和自动更新说明](https://github.com/slayercjc/GoGoGo/blob/android16-baidu/docs/RELEASING.zh-CN.md)。

## 问题反馈

请到 [Issues](https://github.com/slayercjc/GoGoGo/issues) 提交手机型号、系统版本、幻位版本、问题描述和复现步骤。地图空白请附百度鉴权错误码（如有），**不要提交 AK、签名私钥或密码**。提交截图或日志前请自行隐藏不希望公开的地址、坐标等信息。

## 开源与致谢

本分支没有新增收费解锁、支付 SDK 或订阅功能。百度地图服务的额度和收费以百度开放平台当前规则为准。

感谢原作者 **ZCShou** 和 GoGoGo 贡献者，以及百度地图 SDK、AndroidX、Material Components 等项目。上游版权声明保留，代码按仓库 [GPL-3.0 许可证](https://github.com/slayercjc/GoGoGo/blob/android16-baidu/LICENSE) 分发；第三方组件遵循各自许可证。

本工具用于开发、学习和获授权的定位测试。
