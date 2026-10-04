# 英语单词

一款简洁高效的 Android 英语词汇学习应用，支持多种记忆模式和 AI 扩展。

## 功能特点

### 单词导入

- **简单导入**：直接粘贴文本，支持 `1. hello  n.  你好` 格式（序号、词性可省略，支持词组，也支持 `U.S.`、`3D`、`2024` 这类词头）
- **文件导入**：支持 TXT / DOCX 文件导入；TXT 带 UTF-8 / UTF-16LE BOM 时去除 BOM 并直接按对应编码解码，不再严格校验或回退；无上述 BOM 时先严格校验 UTF-8，失败则回退 GBK；DOCX 提取正文文本，不保留排版
- 序号支持半角及全角数字，英文词头不做全角规范化；中文课名等前缀会导致解析失败，部分非中文前缀可能被算入词头，请先移除这些前缀
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
- 系统已保存并恢复 Activity 状态时，可恢复本轮题目、题号、作答、得分及错题重练题源；主动退出练习、移除最近任务或强行停止后不支持续练。保存数据受 Android Bundle 容量限制，超大释义或错题表可能无法恢复。参见 [Android 状态保存说明](https://developer.android.com/topic/libraries/architecture/saving-states)
- 已知限制：AI 选项尚未生成时旋转屏幕或发生系统重建，未完成的生成任务不会恢复，部分题目可能一直没有选项；需退出后重新开始练习

### 错题本

- 答错自动收录，答对自动移出
- 同一词条在四种模式间共用错题记录，可选择任一模式重练
- 按首字母分组排序，支持单条移出和清空；单条移出后可撤销，撤销按原值还原，错误次数与最近答错时间不变

### AI 扩展（可选）

- 四选一干扰项优先由 AI 生成；题目先立即显示，选项再按题异步补齐。未配置、关闭 AI、超时、请求失败或候选不足时，依次从词库和内置列表补足，因此个别题目可能用的是兜底选项
- 每轮用 4 个并发许可限制选项生成任务；任务取得许可后设置 10 秒协程超时，排队时间不计入。当前同步网络请求不能随协程及时取消，因此实际等待可能超过 10 秒，这也不是全应用网络请求数量的硬上限
- 单词详情页可查看 AI 生成的多义项和例句
- AI 提示词将固定规则放在前面、动态词条放在末尾；干扰项保留 2 组题型 JSON 示例以复用前缀，两种题型都使用词表中的真实中文释义。详细释义恢复为原来的无示例短模板，以减少输入
- 仅当请求使用 HTTPS、主机为 `api.deepseek.com`、端口为 443、模型名称精确为 `deepseek-flash` 时，详细释义请求添加 `thinking.type=disabled`，仍要求最多 5 个常见义项、英文例句及中文翻译，保留 2048 token 输出上限和 0.3 温度；其他服务和模型不添加该参数。干扰项及测试连接不设置思考开关，沿用服务端默认行为，干扰项关闭思考的费用对照未接入应用。参见 [DeepSeek 思考模式说明](https://api-docs.deepseek.com/guides/thinking_mode/)
- 设置页可查看最近一次 HTTP 成功且答案文本非空的 AI 业务响应的任务、服务域名、配置模型、完成时间、输入/输出 token、缓存命中/未命中 token 与命中率；它不是累计用量，生成结果解析失败也可能更新该记录。统计只保留在内存中，应用进程重启后清除。“测试连接”保持原有的耗时与成功/失败提示，不计入业务统计；服务未返回某项计数时显示“服务未提供”，与明确返回的 `0` 区分
- 历史验证：2026-10-04 加示例后两类模板均观察到非零命中；2026-10-05 精简为 2 组干扰项、1 组释义示例后，六个相同词条的输入由 352–361 降至 267–280 token，均命中 128 token。这是随后恢复短释义模板之前的结果，不能代表当前释义请求
- 2026-10-05 恢复短释义模板并关闭思考的初步对照：六个词条各两轮、共 12 对（24 次请求），两组缓存命中均为 0，均完整返回且 JSON 结构检查通过。相同短模板下，默认思考与关闭思考的平均输入分别为 154.3 / 129.3 token，平均输出为 442.3 / 92.1 token；按当时 [官方非高峰定价](https://api-docs.deepseek.com/quick_start/pricing/)估算，这组样本的总费用降低 74.1%。开发端初步阅读了 12 条非思考回答，未见明显错误，但未做充分的词典质量评测，不能保证义项完整、例句与输入词性一致，或今后的质量、耗时及费用
- 干扰项示例仍会增加相对无示例短模板的输入长度；缓存计数和未命中计数应结合输出用量及定价评估，不能仅凭命中率判断费用
- 前缀复用不保证每次缓存命中：服务端可能尚未建立缓存、不缓存或清理缓存，命中数可以为 `0`；应用不添加无意义填充，也不发送额外预热请求。参见 [DeepSeek 上下文缓存说明](https://api-docs.deepseek.com/guides/kv_cache)
- 支持 OpenAI 兼容的 Chat Completions API；首次使用时 API 地址、模型名称和 API Key 均为空，需自行填写。地址请使用 HTTPS，并包含服务要求的基础路径（如 `/v1`）；程序只追加 `/chat/completions`，也接受已完整填写的该端点。当前配置不允许明文 HTTP，参见 [Android 网络安全配置](https://developer.android.com/privacy-and-security/security-config)
- API 三项输入需点击“保存配置”才会更新配置；“测试连接”使用当前输入发起请求，不代替保存；AI 开关和每轮题数在更改时立即更新并触发保存

### 单词本管理

- 搜索、删除、清空；单条删除可撤销，词条与删除时联动清理的错题记录会一并按原值恢复
- 已练习词条按历史正确率显示掌握度圆点，颜色随主题变化；未练习词条不显示圆点
- 数据读取失败时首页会提示，本次以空词库、空错题本或默认配置启动，并尝试备份原文件为 `<文件名>.corrupt-<时间戳>`；备份失败不会单独上报，界面的“已备份”提示不保证备份成功。保存失败也会弹出提示
- 后台保存会合并尚未处理的信号以减少重复写入；正常路径用临时文件重命名替换，失败回退可能删除旧文件，进程终止也可能丢失尚未落盘的修改

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
| versionName / versionCode | 1.2 / 3 |

本项目的构建基线为 **JDK 17**。[AGP 8.7 最低要求 JDK 17](https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes)，[Gradle 8.10.2 支持使用 Java 8–23 运行](https://docs.gradle.org/8.10.2/userguide/compatibility.html)，因此 JDK 26 不兼容当前 Wrapper 构建组合。若机器默认版本较新，请按下文示例显式把 `JAVA_HOME` 指向 17，可用 `"$JAVA_HOME/bin/java" -version` 确认。Gradle 8.10.2 由项目自带的 wrapper 提供，首次执行 `./gradlew` 会自动下载，无需另行安装。

## 编译步骤

### 1. 克隆项目

```bash
git clone https://github.com/jrcloud-next/EnglishWords
cd EnglishWords
```

克隆后得到的目录名为 `EnglishWords`。以下命令均在包含 `settings.gradle.kts` 的项目根目录执行，并将示例中的 JDK / SDK 路径替换为你机器上的实际路径。**Windows 用户**请对照本节末尾的「Windows 差异」。

### 2. 配置 Android SDK

在项目根目录创建或编辑 `local.properties`，填写已安装的 Android SDK 路径。该 SDK 需包含 **API 35 平台**与 **Build-Tools 34.0.0**；项目未指定 `buildToolsVersion`，采用 [AGP 8.7 的默认版本 34.0.0](https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes)，`compileSdk = 35` 不代表默认 Build-Tools 为 35.0.0：

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

**使用新签名自己编译 Release 包**时，先生成密钥库（仓库里没有 `signing/` 目录，需要先创建）。若需更新已发布的应用，使用原签名材料，跳过生成步骤：

```bash
export JAVA_HOME="/path/to/jdk-17"
mkdir -p signing
"$JAVA_HOME/bin/keytool" -genkeypair -v \
  -keystore signing/release.keystore -alias byjr \
  -storetype PKCS12 \
  -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=YourName, C=CN"
```

按交互提示输入并确认库密码，避免把密码写进命令历史。然后在 `local.properties` 中追加两行，保留已有的 `sdk.dir`：

```properties
storePassword=你的密码
keyPassword=你的密码
```

以上 JDK 17 `keytool` 示例显式生成 PKCS12，并使用同一密码保护库和密钥，因此两行填相同值；已有密钥库应填写其实际库密码与密钥密码，不能仅凭 `.keystore` 扩展名推断格式或密码关系。参见 [JDK 17 keytool 说明](https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html)。

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
| 生成密钥库 | `"$JAVA_HOME/bin/keytool" …` | PowerShell：`& "$env:JAVA_HOME\bin\keytool.exe" …`<br>cmd：`"%JAVA_HOME%\bin\keytool.exe" …`；把第 4 步的参数合并成一行，密码仍按提示输入 |

PowerShell 的 `&` 用于执行引号内的程序路径，后续参数放在引号外；参见 [PowerShell 调用运算符说明](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_operators#call-operator-)。

`local.properties` 中的 SDK 路径建议用**正斜杠**：Java 的 properties 文件里反斜杠是转义符，`C:\Users\...` 会被读成 `C:Users...`。

```properties
sdk.dir=C:/Users/你的用户名/AppData/Local/Android/Sdk
```

## 项目结构

```
app/src/main/
├── AndroidManifest.xml
├── java/com/jr/englishword/
│   ├── MainActivity.kt            # 入口：单 Activity + 手写路由；Snackbar 与落盘失败提示
│   ├── data/
│   │   ├── Models.kt              # WordEntry / WrongRecord / AppSettings / QuizMode
│   │   ├── Parser.kt              # 词表行解析、DOCX 正文提取、TXT 编码自适应
│   │   └── Repository.kt          # 单例；StateFlow + JSON 持久化（合并写、临时文件重命名、读写错误上报）
│   ├── net/
│   │   ├── Api.kt                 # OpenAI 兼容 Chat Completions 客户端
│   │   ├── AiChatRequest.kt       # 标准请求构造；仅官方 Flash 详细释义关闭思考
│   │   ├── AiPrompts.kt           # 固定规则前缀与动态词条 JSON
│   │   ├── AiResponseParsing.kt   # 干扰项数组和详细释义对象解析
│   │   └── AiUsage.kt             # 服务端用量与缓存统计、内存诊断快照
│   └── ui/
│       ├── AppViewModel.kt        # AndroidViewModel，转发 Repository 与 AI 调用
│       ├── QuizSession.kt         # 答题会话模型 + rememberSaveable Saver（系统状态恢复时保留本轮）
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
