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

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material3（Android 12+ 系统动态取色，支持跟随系统的深色模式）
- **架构**：单 Activity + 手写路由
- **序列化**：kotlinx.serialization
- **网络**：OkHttp
- **最低版本**：Android 8.0（API 26）

## 界面

- 首页为**渐变主色统计卡**（词库单词 / 已掌握 / AI 状态，数字带滚动动画），下方为四种记忆模式入口、错题本横幅与三个快捷入口
- 四个记忆模式各有一个固定的分类识别色（靛蓝 / 青 / 琥珀 / 粉，做成的渐变图标块），深色模式下自动换成对比度更高的浅色调；其余界面元素全部跟随系统动态取色
- 答题结束的成绩页使用**渐变圆形进度环**展示正确率；答题选项在出现时错落入场，对/错状态平滑变色
- 深色模式跟随系统：Android 12+ 使用 `dynamicDarkColorScheme`，低版本使用内置深色方案；启动窗口也已配 `values-night` 主题，避免冷启动闪白
- 圆角、字阶、语义色（答题正确绿）与渐变停靠点统一由主题提供，不再散落在各页面
- 动效遵循"明显但不循环"：只有入场、按压回弹、变色与进度生长，没有常驻重绘动画

## 编译环境

| 组件 | 版本 |
|------|------|
| JDK | 17 |
| Gradle | 8.10.2 |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.10.01 |
| compileSdk | 35 |
| versionName / versionCode | 1.1 / 2 |

`gradle/wrapper` 已指向 Gradle 8.10.2，与本机构建基线一致，可直接使用 `./gradlew`；下文示例显式调用本机 Gradle 8.10.2，两者目标版本相同。构建时请指定 JDK 17。

> 当前源码 `versionName`/`versionCode` 与 SettingsScreen「关于」文案均为 v1.1，三者一致。仅修改文档不需要递增版本号；修改应用代码后请按同一流程同步这三处。

## 编译步骤

### 1. 克隆项目

```bash
git clone '<repo-url>' EnglishWordsByJR
cd EnglishWordsByJR
```

将 `<repo-url>` 替换为实际仓库地址。以下命令均在包含 `settings.gradle.kts` 的项目根目录执行，并将示例中的工具路径替换为本机实际路径。

### 2. 配置 Android SDK

在项目根目录创建或编辑 `local.properties`，填写已安装的 Android SDK 路径（需包含 API 35 平台）：

```properties
sdk.dir=/path/to/android-sdk
```

Debug 编译不需要下面的 Release 密钥库和密码。

### 3. 编译 Debug APK

```bash
export JAVA_HOME="/path/to/jdk-17"
"/path/to/gradle-8.10.2/bin/gradle" :app:assembleDebug --console=plain
```

构建成功后的产物路径：`app/build/outputs/apk/debug/app-debug.apk`。

### 4. 配置 Release 签名

Release 编译需要将密钥库放在 `signing/release.keystore`，其中必须包含别名为 `byjr` 的签名密钥。在已有 `local.properties` 中追加对应密码，保留前面的 `sdk.dir`：

```properties
storePassword=你的密钥库密码
keyPassword=你的密钥密码
```

`local.properties` 和 `signing/` 下的密钥库已在 `.gitignore` 中排除，不要提交密码或密钥库。

更新已有应用时需沿用原签名密钥；这些签名材料不随仓库提供。

### 5. 编译 Release APK

```bash
export JAVA_HOME="/path/to/jdk-17"
"/path/to/gradle-8.10.2/bin/gradle" :app:assembleRelease --console=plain
```

构建成功后的产物路径：`app/build/outputs/apk/release/app-release.apk`。若构建失败，请先处理错误；目录中遗留的 APK 不代表本次构建成功。

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

- API 配置以未额外加密的 JSON 存储在应用私有目录的 `api_settings.json` 中；私有目录隔离不等于加密保护
- 测试连接和 AI 请求会将 API Key 通过 `Authorization: Bearer` 请求头发送到用户配置的 API 服务进行认证；生成干扰项或详细释义时，请求还会包含当前单词、词性或释义等相关内容
- 当前清单启用了 `android:allowBackup="true"`，且没有排除配置文件；依据设备和系统的备份设置，配置及词库等数据可能进入系统备份或设备迁移，不能保证数据始终只在原设备内。参见 [Android 自动备份规则](https://developer.android.com/identity/data/autobackup)
- 应用首次启动未内置 API 密钥；不要把个人 API Key、签名密码或密钥库提交到仓库
- Release 构建启用 R8 混淆

## 许可

本项目仅供学习交流使用。
