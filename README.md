# 英语单词

一款简洁高效的 Android 英语词汇学习应用，支持多种记忆模式和 AI 扩展。

## 功能特点

### 单词导入
- **简单导入**：直接粘贴文本，支持 `1. hello  n.  你好` 格式（序号、词性可省略，支持词组）
- **文件导入**：支持 TXT / DOCX 文件导入，TXT 自动识别 UTF-8/GBK 编码
- 导入前可预览解析结果，支持去重和清空词库后导入

### 四种记忆模式
| 模式 | 说明 |
|------|------|
| 英译中 | 看英文，四选一选中文释义 |
| 中译英 | 看中文，四选一选英文单词 |
| 默写中文 | 看英文，输入中文释义 |
| 拼写英文 | 看中文，默写英文单词（提示首字母） |

- 答对自动跳转，答错停留展示正确答案
- 每轮题数可选 5/10/15/20
- 结束后显示成绩和错词回顾

### 错题本
- 答错自动收录，答对自动移出
- 支持按模式错题重练
- 按首字母分组排序，支持单条移出（可撤销）和清空

### AI 扩展（可选）
- 四选一干扰项由 AI 智能生成
- 单词详情页可查看 AI 生成的多义项和例句
- 支持 OpenAI 兼容 API（默认适配 DeepSeek）

### 单词本管理
- 搜索、删除、清空
- 掌握度圆点标识（绿/黄/灰）

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

## 编译步骤

### 1. 克隆项目

```bash
git clone <repo-url>
cd EnglishWordsByJR
```

### 2. 配置签名（可选，Release 编译需要）

在项目根目录创建 `local.properties`，添加签名密码：

```properties
sdk.dir=/path/to/android-sdk
storePassword=你的密钥库密码
keyPassword=你的密钥密码
```

> ⚠️ `local.properties` 包含敏感信息，已在 `.gitignore` 中排除，不要提交到仓库。

将签名密钥库放入 `signing/` 目录：

```
signing/release.keystore
```

### 3. 编译 Release APK

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleRelease --console=plain
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

### 4. 编译 Debug APK

```bash
./gradlew assembleDebug --console=plain
```

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

- API Key 仅存储在设备私有目录，不上传任何服务器
- 源代码中不包含任何硬编码的密钥或凭证
- Release 构建启用 R8 混淆

## 许可

本项目仅供学习交流使用。
