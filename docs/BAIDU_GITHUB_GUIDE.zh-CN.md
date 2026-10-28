# 小米 15 Pro：百度地图接入与 GitHub 云编译

适用设备：小米 15 Pro，HyperOS 3.0.309.0。你的现象是原版能够打开，但地图不显示。

这份文档对应本次提供的 GoGoGo v1.12.2 Android 16 候选补丁。配置已准备好，但还没有完成云端编译或手机验收。

你的仓库为 [slayercjc/GoGoGo](https://github.com/slayercjc/GoGoGo)。连接器现已安装并已成功创建 android16-baidu 分支。源码由助手提交后，可以跳过第 1～3 步，直接准备签名和百度 AK。

## 先了解三件事

1. 地图空白不一定由 Android 16 导致。百度地图需要有效的 AK，并校验 APK 的包名和签名。原项目携带的作者 AK 是否仍可用，目前无法从源码判断。
2. **AK、包名、签名 SHA1 必须对应同一个安装包。** 本教程一直用你自己的一份签名证书，避免每次云编译改变 SHA1。
3. GitHub 可以替电脑下载 Android SDK 并编译。你本地只需要 Git 和 JDK 17，用于准备代码与签名；不必安装 Android Studio。首次操作需要登录你自己的 GitHub 和百度账号。

完成后的关系如下：

| 项目 | 本教程使用的值 |
| --- | --- |
| 百度应用类型 | Android SDK |
| APK 包名 | com.zcshou.gogogo |
| 发布签名文件 | 你创建的 gogogo-release.jks |
| 签名别名 | gogogo |
| 发布版 SHA1 | 从上述证书读取 |
| AK | 你在百度控制台申请 |
| GitHub Workflow | Android 16 build |

## 第 1 步：准备自己的 GitHub 仓库

1. 登录 [GitHub](https://github.com/)。
2. 打开 [原项目](https://github.com/ZCShou/GoGoGo)。
3. 点击右上角 **Fork**。
4. Owner 选择自己的账号；仓库名可以保留 GoGoGo。
5. 点击 **Create fork**。
6. 你已完成这一步，当前仓库为 https://github.com/slayercjc/GoGoGo 。

注意：Fork 默认分支可能是新版源码，而这次补丁只针对 v1.12.2，不能直接把补丁套在任意最新版上。下面会从指定 tag 建立独立分支。

## 第 2 步：准备 Git 和 JDK 17

1. 从 [Git 官方下载页](https://git-scm.com/downloads/win) 安装 Git for Windows。
2. 从 [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17) 安装 Windows x64 的 JDK 17，选择安装包并启用 JAVA_HOME/PATH 配置选项。
3. 重新打开 PowerShell，分别运行：

```powershell
git --version
java -version
keytool -help
```

预期：能显示 Git 版本、Java 17 版本和 keytool 帮助。如果提示找不到命令，先修好 PATH，不要跳过。

## 第 3 步：应用修改到 v1.12.2 分支

下载本次最新版候选包，解压到自己知道的目录。下面用 D:/GoGoGo修改包 举例，需要替换为你的实际路径。

在用于存放工程的目录打开 PowerShell，执行：

```powershell
git clone https://github.com/ZCShou/GoGoGo.git GoGoGo-android16
cd GoGoGo-android16
git switch -c android16-baidu v1.12.2
git apply --check "D:/GoGoGo修改包/GoGoGo-android16.patch"
git apply "D:/GoGoGo修改包/GoGoGo-android16.patch"
git diff --stat
```

预期：check 没有报错，修改内容包含源码和 .github/workflows/android16.yml。

把 origin 改成你自己的 Fork，再提交：

```powershell
git remote set-url origin https://github.com/slayercjc/GoGoGo.git
git add .
git commit -m "Adapt Android 16 and add Baidu signed build workflow"
git push -u origin android16-baidu
```

第一次提交若提示身份未设置，按 Git 的提示配置你自己的 user.name、user.email；推送时通过 GitHub 的登录提示完成认证。

推送到 android16-baidu 分支会自动触发不需要签名 Secrets 的编译检查。首次 Fork 若工作流未启用，先进入 Actions 按提示启用，再推送新提交或按后面的手动步骤执行。

不要把 jks、密码、base64 签名内容放进工程目录。后面的签名文件放在独立目录。

### 让手动编译按钮出现

GitHub 的 workflow_dispatch 通常需要工作流文件存在于仓库默认分支。

对这个专门用于适配的 Fork，进入 **Settings → General → Default branch**，把默认分支改成 **android16-baidu**。不同 GitHub 界面版本可能把默认分支设置放在 Branches；找到 Default branch 即可。

这只改变你 Fork 的默认分支，不修改原作者仓库。如果 Fork 已用于其他工作，不要改默认分支，改用 PR 把工作流放到原默认分支。

## 第 4 步：先运行一次不需要 AK 的编译检查（可选）

1. 打开自己的仓库 → **Actions**。
2. 如果出现启用 Fork 工作流提示，按页面提示启用。
3. 左侧选 **Android 16 build**。
4. 点击 **Run workflow**。
5. Branch 选 android16-baidu，mode 选 **check**。
6. 点击绿色运行按钮。

check 会编译 Debug、运行已有单元测试和 Lint，不上传供安装的 APK。它不能验证百度在线鉴权或真机定位。

如果失败，打开红色任务，复制最先出现的构建错误给我，或发 Actions 运行链接。不要复制 Secrets 的内容。

## 第 5 步：生成你自己的固定签名证书

在工程以外新建一个目录，例如 Documents/GoGoGoSigning，然后在该目录打开 PowerShell：

```powershell
keytool -genkeypair -v -keystore gogogo-release.jks -storetype JKS -alias gogogo -keyalg RSA -keysize 2048 -validity 10000
```

按提示操作：

1. 设置一个至少 6 位、足够强且你能妥善保存的密钥库密码。
2. 填写证书姓名/组织/城市等信息；这些是证书信息，不是百度账号密码。
3. 确认信息。
4. 如果提示设置别名 gogogo 的密钥密码，直接回车，使用与密钥库相同的密码，后续配置更简单。

预期：目录中出现 gogogo-release.jks。

**备份这个文件和密码。** 后续更新使用同一份证书，否则签名 SHA1 会改变，也可能无法覆盖安装。

## 第 6 步：读取 SHA1

在同一目录运行：

```powershell
keytool -list -v -keystore gogogo-release.jks -alias gogogo
```

输入刚设置的密码，找到类似：

```text
SHA1: AA:BB:CC:...（实际是 20 组十六进制数）
```

复制 SHA1 后面的完整内容，保留冒号。不要复制 SHA256，也不要填 MD5。

## 第 7 步：在百度创建 Android SDK 应用

1. 打开 [百度地图开放平台](https://lbsyun.baidu.com/)。
2. 登录自己的百度账号。
3. 按控制台提示完成开发者注册/认证。个人学习用途选择适合你的个人身份；具体要求以页面为准。
4. 进入 **控制台 → 应用管理 → 我的应用 → 创建应用**。
5. 应用名称可填：影梭个人测试。
6. **应用类型选择 Android SDK**。不要选浏览器端或服务端。
7. 包名填：

```text
com.zcshou.gogogo
```

8. **发布版 SHA1** 填第 6 步读取的 SHA1。本教程下载的是自己签名的 release APK，因此必须匹配发布版签名。
9. 开发版 SHA1 如果可选可暂时不填；若表单要求填写，可先填同一份证书 SHA1。以后另用本机 Debug 签名时，要替换为 Debug 的实际 SHA1。
10. 勾选地图显示、检索、定位等所需服务。服务名称以当前后台为准，不需要为了本项目额外选择导航/付费增值服务。
11. 提交后，找到生成的 **AK**，复制保存。

如果后台使用一个“安全码”输入框，按页面要求填写 **SHA1;包名**，中间为英文分号。例如：

```text
你的SHA1;com.zcshou.gogogo
```

如果是分开的 SHA1、包名输入框，就分别填写，不要拼接。

当前百度官方说明境内地图 SDK 非商业使用免费；商业使用需要授权。其他服务的额度、费用以其当前规则为准。

## 第 8 步：把签名文件转换成 Base64

在签名文件所在目录运行：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path '.\gogogo-release.jks').Path)) | Set-Clipboard
```

这会把完整 Base64 文本放进剪贴板，不显示在终端。**Base64 是私钥文件的编码，不是加密**，只粘贴到下一步指定的 GitHub Secret，不要放进 Issue、聊天或源码。

## 第 9 步：配置 5 个 GitHub Secrets

打开自己的仓库：

**Settings → Secrets and variables → Actions → Secrets → New repository secret**

逐个创建，下列名称必须完全一致，大小写也一致：

| Name | Secret 填什么 |
| --- | --- |
| BAIDU_MAP_AK | 第 7 步申请的 Android SDK 类型 AK |
| ANDROID_KEYSTORE_BASE64 | 第 8 步复制到剪贴板的整段文本 |
| ANDROID_STORE_PASSWORD | 第 5 步的密钥库密码 |
| ANDROID_KEY_ALIAS | gogogo |
| ANDROID_KEY_PASSWORD | 第 5 步别名密钥密码；若回车使用相同密码，就填密钥库密码 |

每个填写完点击 Add secret。要创建在 **Secrets** 中，不是 Variables。它们不需要发给我。

## 第 10 步：云端编译正式签名 APK

1. 仓库 → Actions → Android 16 build。
2. Run workflow。
3. Branch 选 android16-baidu。
4. mode 选 **signed-apk**。
5. 点击运行，等待完成。

流程会先检查 5 个 Secret 是否齐全，编译和检查代码，再构建 release APK，使用你提供的证书签名，验证签名与 APK ZIP 对齐，最后上传产物。

它不会自动发布 GitHub Release。若是私有仓库，请留意你账号的 Actions 用量额度；以 GitHub Billing 页面为准。

## 第 11 步：下载 APK 并核对签名

运行成功后，在运行详情页找到 **Artifacts**：

1. 下载 **GoGoGo-Android16-signed**。
2. 解压，得到 GoGoGo-android16-candidate.apk、signature-report.txt、zip-alignment.txt、SHA256SUMS.txt。
3. 打开 signature-report.txt，找到 signer certificate SHA-1 digest，核对它与百度后台发布版 SHA1 是同一个值；冒号和大小写的显示差异不影响值。
4. 把 APK 传到手机安装。

新签名与原版不同，可能提示签名冲突。此时先处理原版本地历史记录，再卸载原版后安装新包。不要为解决冲突随意重新生成签名证书。

## 第 12 步：小米手机上验证地图

1. 打开应用并阅读、同意隐私说明。
2. 允许定位；通知建议允许，便于使用运行中的控制入口。
3. 确认系统允许影梭使用 WLAN 和移动数据。HyperOS 菜单名称会随版本变化，可从应用信息页寻找联网控制。
4. 先观察底图是否出现，再测试搜索地点。地图显示本身不要求启用“模拟位置应用”。
5. 若提示“百度地图鉴权失败（数字）”，记录完整数字，重点核对 AK、包名、实际 APK 签名、服务勾选。
6. 若提示网络错误，换一个正常网络重试，检查应用联网权限。暂时停用会改变该应用网络请求的代理进行对比。
7. 底图正常后再开启开发者选项 → 选择模拟位置信息应用 → 影梭，授予悬浮窗权限，从主界面启动定位。
8. 测试切后台、锁屏、返回前台。后台中断与地图鉴权是不同问题，需要分别记录现象。

如果仍空白，把以下信息发给我：

- 本次 GitHub Actions 运行链接；
- 手机显示的鉴权/网络提示；
- 页面是白屏、网格、黑屏，还是有百度标识但没有地图；
- 搜索地点是否能返回结果。

不要提供 AK、私钥或密码。

## 常见错误

| 现象 | 处理 |
| --- | --- |
| 找不到 Run workflow | 确认工作流存在于默认分支，且已启用 Fork Actions |
| Missing repository secret | 按错误列出的名称补齐对应 Secret |
| base64 解码失败 | 用第 8 步重新生成，粘贴完整内容 |
| 密钥库密码错误 / alias 不存在 | 检查密码与别名 gogogo，别混入账号密码 |
| Gradle、SDK 或依赖下载失败 | 打开失败步骤区分网络错误与版本/编译错误，提供运行链接 |
| 编译成功但地图空白 | 编译不会验证百度账号鉴权；核对实际 APK 签名 SHA1、包名及 AK |
| 签名冲突无法安装 | 新旧 APK 签名不同，先备份数据再卸载旧版 |
| 出现 16 KB / 原生库错误 | 需要检查最终 SO 的 ELF 对齐与供应商版本，ZIP 对齐通过不等于 SO 已兼容 |

## 当前验证边界

已准备源代码、错误提示和手动触发的云编译流程；尚未在你的仓库运行，也未在小米 15 Pro 上测试。本地缺少 Java/Android SDK。源码修改和云编译成功都不能单独证明地图已经在手机上修好。

官方参考：

- [百度 AK 申请](https://lbs.baidu.com/docs/guide?title=android-guide%2Fcreate-project%2Fak)
- [百度地图显示与 AK 配置](https://lbs.baidu.com/docs/android?title=androidsdk%2Fguide%2Fcreate-map%2Fshowmap)
- [百度地图 FAQ](https://lbsyun.baidu.com/index.php?title=模板:FAQ-Android-map)
- [GitHub Secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
- [GitHub 手动运行 Workflow](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)
