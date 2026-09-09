# Quick Calculation

Quick Calculation（速算练习）是一个面向公务员行测「资料分析」模块的 Android 刷题 App。项目通过规则驱动与约束随机生成题目，帮助用户练习基期量、现期量、增长量、增长率、比重、平均数、倍数和特殊增长率等常见资料分析题型。

> 本项目为个人开发学习产品，源码开放用于学习、交流和非商业研究。禁止任何形式的商业使用、转售、商业培训包装、闭源集成或商业化分发。

## 项目简介

资料分析题目的核心是围绕现期量、基期量、增长量和增长率进行换算，并扩展到比重、平均数、倍数等综合题型。本项目不是维护固定题库，而是基于题材库、公式规则和难度参数自动生成题目，并提供即时判分、正确答案和解析。

核心目标：

- 自动生成大量资料分析练习题，降低固定题库维护成本。
- 按题型和难度生成更接近真实考试风格的数据。
- 支持选择题和填空题两种练习方式。
- 提供即时反馈、答案解析和正确率统计。

## 技术栈

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Material 3
- Navigation Compose
- AndroidX Lifecycle ViewModel
- Kotlin Coroutines Flow
- DataStore Preferences
- JUnit

## 功能列表

- 题型选择：支持多选资料分析题型。
- 难度选择：支持简单、中等、困难三档。
- 自动出题：根据题型和难度随机生成题目。
- 选择题模式：自动生成 4 个选项，包含正确答案和干扰项。
- 填空题模式：输入数值后按容差自动判分。
- 答案解析：答题后展示正确答案和计算过程。
- 答题统计：记录已答数量、正确数量和正确率。
- 主题切换：支持浅色、深色和跟随系统。
- 本地设置保存：使用 DataStore 保存主题偏好。

## 题型范围

- 基期与现期
- 增长量
- 一般增长率
- 比重
- 平均数
- 倍数
- 特殊增长率

## 使用说明

### 环境要求

- Android Studio
- JDK 17
- Android SDK Platform 36
- Gradle Wrapper（项目已包含 `gradlew`）

### 获取项目

```bash
git clone git@github.com:Yinghao829/Quick-Calculation.git
cd Quick-Calculation
```

### 编译项目

```bash
./gradlew assembleDebug
```

编译成功后，Debug APK 通常位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 运行测试

```bash
./gradlew test
```

### 安装到设备

确保已连接 Android 设备或已启动模拟器：

```bash
./gradlew installDebug
```

也可以手动安装生成的 APK：

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 部署说明

### Debug 部署

Debug 版本适合本地开发、真机调试和功能验证：

```bash
./gradlew assembleDebug
```

生成 APK 后，可通过 Android Studio、Gradle 或 `adb` 安装到测试设备。

### Release 构建

生成 Release 包：

```bash
./gradlew assembleRelease
```

如需正式分发，需要配置 Android 签名信息。建议通过 `keystore.properties` 或 CI 环境变量管理签名配置，不要将密钥文件、签名密码或敏感配置提交到仓库。

### 分发限制

本项目禁止商业化部署和商业化分发。仅允许用于个人学习、代码研究、非商业演示和非商业交流。

## 项目结构

```text
.
├── app/                         # Android 应用模块
│   └── src/main/java/com/example/quickcalculation
│       ├── data/                # 本地数据与设置持久化
│       ├── domain/              # 题目模型、生成器、判分逻辑
│       ├── ui/                  # Compose 页面与展示逻辑
│       ├── ui/theme/            # 主题与配色
│       ├── viewmodel/           # UI 状态管理
│       └── MainActivity.kt      # 应用入口与导航
├── docs/                        # 产品规划、题目生成、UI 设计文档
├── gradle/                      # Gradle Wrapper 与版本目录
├── build.gradle.kts             # 根构建配置
├── settings.gradle.kts          # 项目模块配置
└── README.md                    # 项目说明
```

## 开发说明

- 核心出题入口位于 `QuestionGenerator`。
- 各题型生成器位于 `domain/generator`。
- 判分逻辑位于 `AnswerJudge`。
- Compose 页面位于 `ui`。
- ViewModel 状态流转位于 `QuizViewModel`。

## License 开源协议

本项目采用自定义非商业开源许可：

- 允许个人学习、阅读、修改和非商业用途分发。
- 允许在非商业技术分享、课程作业、个人作品集中引用或演示。
- 禁止商业使用，包括但不限于售卖、商业培训、付费课程打包、商业 App 集成、SaaS 服务集成、广告变现产品集成。
- 禁止移除原作者署名、项目来源和非商业限制声明。
- 如需商业授权，请先联系项目作者并取得书面许可。

由于本许可包含非商业限制，它可能不符合部分组织对 OSI 标准开源协议的定义；本项目的实际授权边界以本节声明为准。
