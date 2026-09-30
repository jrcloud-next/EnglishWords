# 英语单词

一款简洁高效的 Android 英语词汇学习应用，支持多种记忆模式和 AI 扩展。

## 功能特点

### 单词导入

- **简单导入**：直接粘贴文本，支持 `1. hello  n.  你好` 格式（序号、词性可省略，支持词组）
- **文件导入**：支持 TXT / DOCX 文件导入；TXT 优先识别 UTF-8，校验失败时回退 GBK，另支持带 BOM 的 UTF-16LE；DOCX 提取正文文本，不保留排版
- 导入前可预览解析结果；单次解析按“英文单词 + 词性”（忽略大小写）去重，保留首次出现的条目；追加导入使用同一规则与现有词库去重
- 支持清空词库后导入；当前替换导入不会清理旧错题记录

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

### 错题本

- 答错自动收录，答对自动移出
- 同一词条在四种模式间共用错题记录，可选择任一模式重练
- 按首字母分组排序，支持单条移出和清空；单条移出后可撤销，但当前实现会重新建立记录，不恢复原错误次数和时间

### AI 扩展（可选）

- 四选一干扰项优先由 AI 生成；未配置、关闭 AI、请求失败或候选不足时，依次从词库和内置列表补足
- 单词详情页可查看 AI 生成的多义项和例句
- 支持 OpenAI 兼容的 Chat Completions API；首次使用时 API 地址、模型名称和 API Key 均为空，需自行填写
- API 三项输入需点击“保存配置”才会更新配置；“测试连接”使用当前输入发起请求，不代替保存；AI 开关和每轮题数在更改时立即更新并触发保存

### 单词本管理

- 搜索、删除、清空；单条删除可撤销，但只恢复词条，不恢复删除时移除的错题记录
- 已练习词条按历史正确率显示掌握度圆点，颜色随主题变化；未练习词条不显示圆点

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material3
- **架构**：单 Activity + 手写路由
- **序列化**：kotlinx.serialization
- **网络**：OkHttp
- **最低版本**：Android 8.0（API 26）

## 编译环境

| 组件 | 版本 |
|------|------|
| JDK | 17 |
| Gradle | 8.10.2 |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.10.01 |
| compileSdk | 35 |

以上 Gradle 8.10.2 为历史构建基线。**当前 Gradle Wrapper 指向 9.6.0，尚未验证与本项目的兼容性。** 下文明确调用本机 Gradle 8.10.2，并指定 JDK 17；不要直接将命令替换为 `./gradlew`。本次仅核对文档与配置，未重新构建。

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
app/src/main/java/com/jr/englishword/
├── MainActivity.kt          # 入口，路由管理
├── data/
│   ├── Models.kt            # 数据模型
│   ├── Parser.kt            # 词表解析
│   └── Repository.kt        # 数据持久化
├── net/
│   └── DeepSeekApi.kt       # AI API 客户端
└── ui/
    ├── AppViewModel.kt      # 状态管理
    ├── theme/Theme.kt       # 主题配色
    └── screens/
        ├── HomeScreen.kt    # 首页
        ├── ImportScreen.kt  # 导入
        ├── QuizScreen.kt    # 答题
        ├── ListScreen.kt    # 单词本
        ├── WrongBookScreen.kt # 错题本
        └── SettingsScreen.kt  # 设置
```

## 安全说明

- API 配置以未额外加密的 JSON 存储在应用私有目录的 `api_settings.json` 中；私有目录隔离不等于加密保护
- 测试连接和 AI 请求会将 API Key 通过 `Authorization: Bearer` 请求头发送到用户配置的 API 服务进行认证；生成干扰项或详细释义时，请求还会包含当前单词、词性或释义等相关内容
- 当前清单启用了 `android:allowBackup="true"`，且没有排除配置文件；依据设备和系统的备份设置，配置及词库等数据可能进入系统备份或设备迁移，不能保证数据始终只在原设备内。参见 [Android 自动备份规则](https://developer.android.com/identity/data/autobackup)
- 应用首次启动未内置 API 密钥；不要把个人 API Key、签名密码或密钥库提交到仓库
- Release 构建启用 R8 混淆

## 许可

本项目仅供学习交流使用。
