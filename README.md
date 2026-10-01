# 英语单词

一款简洁高效的 Android 英语词汇学习应用，支持多种记忆模式和 AI 扩展。

## 功能特点

### 单词导入

- **简单导入**：直接粘贴文本，支持 `1. hello  n.  你好` 格式（序号、词性可省略，支持词组，也支持 `U.S.`、`3D`、`2024` 这类词头）
- **文件导入**：支持 TXT / DOCX 文件导入；TXT 优先识别 UTF-8，校验失败时回退 GBK，另支持带 BOM 的 UTF-16LE；DOCX 提取正文文本，不保留排版
- 导入前可预览解析结果；单次解析按“英文单词 + 词性”（忽略大小写）去重，保留首次出现的条目；追加导入使用同一规则与现有词库去重
- 支持「清空现有单词及其学习记录后导入」；替换导入会整体替换词库并同时清空错题本

### 四种记忆模式

| 模式 | 说明 |
|------|------|
| 英译中 | 看英文，四选一选中文释义 |
| 中译英 | 看中文，四选一选英文单词 |
| 默写中文 | 看英文，输入中文释义 |
| 拼写英文 | 看中文，默写英文单词（提示首字母） |

- 答对自动跳转，答错停留展示正确答案
- 每轮默认 10 题，可选 5/10/15/20，实际题数不超过可用词条数；错题重练不超过当前词库中的错词数量
- 拼写提示保留原词大小写，判分忽略大小写及非字母数字字符
- 结束后显示成绩和错词回顾
- 屏幕旋转或应用被系统回收重建后，本轮题号、作答与得分会保留；错题重练在重建后仍是错题重练

### 错题本

- 答错自动收录，答对自动移出
- 同一词条在四种模式间共用错题记录，可选择任一模式重练
- 按首字母分组排序，支持单条移出和清空；单条移出后可撤销，撤销按原值还原，错误次数与最近答错时间不变

### AI 扩展（可选）

- 四选一干扰项优先由 AI 生成；题目先立即显示，选项再按题异步补齐（同时最多 4 个请求，单题最多等 10 秒）。未配置、关闭 AI、超时、请求失败或候选不足时，依次从词库和内置列表补足，因此个别题目可能用的是兜底选项
- 单词详情页可查看 AI 生成的多义项和例句
- 支持 OpenAI 兼容的 Chat Completions API；首次使用时 API 地址、模型名称和 API Key 均为空，需自行填写
- API 三项输入需点击“保存配置”才会更新配置；“测试连接”使用当前输入发起请求，不代替保存；AI 开关和每轮题数在更改时立即更新并触发保存

### 单词本管理

- 搜索、删除、清空；单条删除可撤销，词条与删除时联动清理的错题记录会一并按原值恢复
- 已练习词条按历史正确率显示掌握度圆点，颜色随主题变化；未练习词条不显示圆点
- 数据文件损坏时首页会给出提示并保留原文件备份（`<文件名>.corrupt-<时间戳>`），不再静默变成空词库；保存失败也会弹出提示

## 界面

- 首页为**渐变主色统计卡**（词库单词 / 已掌握 / AI 状态，数字带滚动动画），下方为四种记忆模式入口、错题本横幅与三个快捷入口
- 四个记忆模式各有固定的分类识别色（靛蓝 / 青 / 琥珀 / 粉），深色模式下自动换成更亮的色调
- Android 12 及以上跟随系统壁纸动态取色，界面配色会随你的壁纸变化
- 答题结束的成绩页用**渐变圆形进度环**展示正确率；答题选项在出现时淡入，答对与答错平滑变色
- 深色模式跟随系统设置，冷启动不会闪白

## 编译环境

| 组件 | 版本 |
|------|------|
| JDK | 17 |
| Gradle | 8.10.2 |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.10.01 |
| compileSdk | 35 |
| minSdk（最低支持） | 26（Android 8.0） |
| versionName / versionCode | 1.1 / 2 |

构建需要 **JDK 17**。AGP 8.7.3 不支持过新的 JDK（实测 JDK 26 会直接报错），若机器默认版本更高，请按下文示例显式把 `JAVA_HOME` 指向 17，可用 `"$JAVA_HOME/bin/java" -version` 确认。Gradle 8.10.2 由项目自带的 wrapper 提供，首次执行 `./gradlew` 会自动下载，无需另行安装。

## 编译步骤

### 1. 克隆项目

```bash
git clone https://github.com/jrcloud-next/EnglishWords
cd EnglishWords
```

克隆后得到的目录名为 `EnglishWords`。以下命令均在包含 `settings.gradle.kts` 的项目根目录执行，并将示例中的 JDK / SDK 路径替换为你机器上的实际路径。**Windows 用户**请对照本节末尾的「Windows 差异」。

### 2. 配置 Android SDK

在项目根目录创建或编辑 `local.properties`，填写已安装的 Android SDK 路径。该 SDK 需包含 **API 35 平台**与 **Build-Tools**（AGP 8.7.3 默认用 35.0.0）：

```properties
sdk.dir=/path/to/android-sdk
```

Debug 编译不需要下面的 Release 密钥库和密码。

### 3. 编译 Debug APK

```bash
export JAVA_HOME="/path/to/jdk-17"
./gradlew :app:assembleDebug --console=plain
```

构建成功后的产物路径：`app/build/outputs/apk/debug/app-debug.apk`。

### 4. 配置 Release 签名

Release 包必须签名。本项目的签名配置固定为：密钥库放在 `signing/release.keystore`、别名 `byjr`、密码从 `local.properties` 读取。

**自己编译 Release 包**时，先生成密钥库（仓库里没有 `signing/` 目录，需要先创建）：

```bash
mkdir -p signing
"$JAVA_HOME/bin/keytool" -genkeypair -v \
  -keystore signing/release.keystore -alias byjr \
  -keyalg RSA -keysize 2048 -validity 10950 \
  -storepass 你的密码 -keypass 你的密码 \
  -dname "CN=YourName, C=CN"
```

然后在 `local.properties` 中追加两行，保留已有的 `sdk.dir`：

```properties
storePassword=你的密码
keyPassword=你的密码
```

密钥库是 PKCS12 格式，库密码与密钥密码实际是同一个值，两行填一样的即可。

**要覆盖安装已发布的应用**：必须改用该应用原有的密钥库与密码，否则签名不一致会安装失败。原签名材料不在本仓库中。

`local.properties` 与 `signing/*.keystore` 都已在 `.gitignore` 中排除，不要提交密码或密钥库。

### 5. 编译 Release APK

```bash
export JAVA_HOME="/path/to/jdk-17"
./gradlew :app:assembleRelease --console=plain
```

构建成功后的产物路径：`app/build/outputs/apk/release/app-release.apk`。Release 构建启用 R8 混淆，产物体积明显小于 Debug 包。若构建失败，请先处理错误；目录中遗留的 APK 不代表本次构建成功。

### Windows 差异

上面的命令是 macOS / Linux 写法，Windows 按下表替换即可：

| 用途 | macOS / Linux | Windows |
|------|---------------|---------|
| 设置 JDK | `export JAVA_HOME="/path/to/jdk-17"` | PowerShell：`$env:JAVA_HOME="C:\path\to\jdk-17"`<br>cmd：`set JAVA_HOME=C:\path\to\jdk-17` |
| 执行构建 | `./gradlew :app:assembleDebug` | `.\gradlew.bat :app:assembleDebug`（cmd 下写成 `gradlew.bat ...`） |
| 创建目录 | `mkdir -p signing` | `mkdir signing`（提示已存在时可忽略） |
| 生成密钥库 | `"$JAVA_HOME/bin/keytool" …` | `"%JAVA_HOME%\bin\keytool" …`，并把第 4 步的多行命令合并成一行 |

`local.properties` 中的 SDK 路径建议用**正斜杠**：Java 的 properties 文件里反斜杠是转义符，`C:\Users\...` 会被读成 `C:Users...`。

```properties
sdk.dir=C:/Users/你的用户名/AppData/Local/Android/Sdk
```

`gradlew.bat` 已随仓库提供，Windows 上无需额外安装 Gradle。

## 项目结构

```
app/src/main/
├── AndroidManifest.xml
├── java/com/jr/englishword/
│   ├── MainActivity.kt            # 入口：单 Activity + 手写路由；Snackbar 与落盘失败提示
│   ├── data/
│   │   ├── Models.kt              # WordEntry / WrongRecord / AppSettings / QuizMode
│   │   ├── Parser.kt              # 词表行解析、DOCX 正文提取、TXT 编码自适应
│   │   └── Repository.kt          # 单例；StateFlow + JSON 持久化（合并写、原子替换、读写错误上报）
│   ├── net/
│   │   └── DeepSeekApi.kt         # OpenAI 兼容 Chat Completions 客户端
│   └── ui/
│       ├── AppViewModel.kt        # AndroidViewModel，转发 Repository 与 AI 调用
│       ├── QuizSession.kt         # 答题会话模型 + rememberSaveable Saver（跨旋转/重建保留本轮）
│       ├── components/
│       │   ├── CommonUi.kt        # IconChip / GradientIconChip / InfoPill / SectionCardHeader
│       │   └── Effects.kt         # GradientHero / CircularRingProgress / GradientBar / AnimatedCounter 等动效组件
│       ├── theme/Theme.kt         # 动态取色（浅/深）+ AppShapes + AppTypography + LocalAppColors
│       └── screens/
│           ├── HomeScreen.kt      # 首页
│           ├── ImportScreen.kt    # 导入
│           ├── QuizScreen.kt      # 答题
│           ├── ListScreen.kt      # 单词本
│           ├── WrongBookScreen.kt # 错题本
│           └── SettingsScreen.kt  # 设置
└── res/
    ├── values/                    # strings / colors / themes（浅色启动主题）
    └── values-night/              # 深色启动主题与 window_bg，避免冷启动闪白
```

## 安全说明

- API 配置以**未额外加密**的 JSON 存放在应用私有目录中；私有目录隔离不等于加密保护
- 「测试连接」和 AI 请求会把 API Key 发送到你配置的 API 服务进行认证；生成干扰项或详细释义时，请求还会包含当前单词、词性或释义
- 应用允许参与系统自动备份，依据设备与系统设置，配置及词库等数据可能进入系统备份或设备迁移，不能保证始终只留在原设备上。参见 [Android 自动备份规则](https://developer.android.com/identity/data/autobackup)
- 应用首次启动未内置任何 API 密钥；不要把个人 API Key、签名密码或密钥库提交到仓库

## 许可

本项目仅供学习交流使用。
