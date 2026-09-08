# 前端 UI 设计规范

> 本文档是刷题 App 前端视觉与交互的实现依据，配合 `PLAN.md` 使用。
> 设计基调：**克制、清爽、少装饰、重信息**——刷题场景下不抢注意力。

---

## 1. 设计原则

| 原则 | 说明 |
|------|------|
| **少卡片、少色块** | 刷题/答题页面尽量不用卡片容器，直接用背景区分层级，减少视觉噪音 |
| **线性图标为主** | 图标以线性（stroke）风格为主，少量线面结合，保持轻盈 |
| **语义色即反馈** | 对错用绿/红，提示用橙，全 App 统一，不另造颜色 |
| **适度动效** | 只做必要的反馈动画（对错颜色 + 轻微缩放 + 换题过渡），不炫技 |

---

## 2. 主题系统

- 支持 **浅色 / 深色** 双主题。
- **应用内可手动切换**（浅色 / 深色 / 跟随系统 三档）。
- 实现方式：Compose `MaterialTheme` + 自定义 `ColorScheme`，通过 `isDark` 切换。

---

## 3. 配色系统（Color Palette）

### 3.1 语义色（两种模式通用）

| 名称 | 色值 | 用途 |
|------|------|------|
| Success（正确） | `#52C41A` | 答对标识、正确选项高亮、正确率 |
| Error（错误） | `#F5222D` | 答错标识、错误选项高亮 |
| Warning（提示） | `#FA8C16` | 提示、待确认状态 |

### 3.2 主色（Brand / Primary）

> ⚠️ 主色待最终确认，当前为建议值。语义色已占用绿/红/橙，主色建议用中性蓝区分。

| 名称 | 浅色 | 深色 | 用途 |
|------|------|------|------|
| Primary（主色） | `#4E6EF2` | `#6C8CFF` | 主按钮、选中态、链接 |
| PrimaryContainer（主色变淡） | `#EAEEFE` | `#2B3350` | 卡片背景、输入框背景、选中容器 |

### 3.3 浅色模式（Light）

| 名称 | 色值 | 用途 |
|------|------|------|
| Background（页面背景） | `#F5F7FA` | 全局页面底色 |
| Surface（卡片/面板底色） | `#FFFFFF` | 卡片、弹层、输入框（白色兜底） |
| TextPrimary（标题文字） | `#1D2129` | 标题、题干重点 |
| TextSecondary（正文文字） | `#4E5969` | 正文、题干描述 |
| TextTertiary（次要文字） | `#86909C` | 辅助说明、时间、单位 |
| Divider（分割线） | `#E5E6EB` | 分割线、描边 |

### 3.4 深色模式（Dark）

| 名称 | 色值 | 用途 |
|------|------|------|
| Background（页面背景） | `#17171A` | 全局页面底色 |
| Surface（卡片/面板底色） | `#26262B` | 卡片、弹层、输入框 |
| TextPrimary（标题文字） | `#F5F5F7` | 标题、题干重点 |
| TextSecondary（正文文字） | `#C9CDD4` | 正文、题干描述 |
| TextTertiary（次要文字） | `#86909C` | 辅助说明（深浅模式可复用） |
| Divider（分割线） | `#2E2E33` | 分割线、描边 |

---

## 4. 排版规范（Typography）

| 层级 | 字号 | 字重 | 用途 |
|------|------|------|------|
| Display | 28sp | Bold | 正确率大数字、结果页 |
| Headline | 20sp | SemiBold | 题干标题、题型名 |
| Body | 16sp | Normal | 题干正文、选项文字 |
| Label | 14sp | Medium | 按钮、标签、chip |
| Caption | 12sp | Normal | 辅助说明、单位、时间 |

> 数值展示（选项、答案）建议用 **等宽/表格数字**（`FontFeature.tabularFigures`），避免数字跳动。

---

## 5. 图标规范

- **风格**：线性（stroke）为主，少量线面结合。
- **尺寸**：导航/操作图标 24dp；语义标识（对/错）20dp。
- **颜色**：图标继承文字色（`TextSecondary`/`TextTertiary`），语义图标用语义色。
- 不使用彩色渐变图标。

---

## 6. 布局规范

- **刷题/答题页**：少用卡片容器，题干直接放在页面背景上，用留白和分割线分层。
- **圆角**：卡片/弹层 `12dp`，输入框/按钮 `8dp`，chip `16dp`。
- **间距**：以 `4dp` 为基准网格（4/8/12/16/24/32）。
- **页面内边距**：左右 16dp，上下 16~24dp。

---

## 7. 组件样式

| 组件 | 样式 |
|------|------|
| 主按钮 | Primary 底色 + 白字，圆角 8dp，按压微缩 |
| 选项按钮（选择模式） | 默认白底（深色为 Surface）+ 分割线描边；选中后语义色描边 + 语义色变淡填充 |
| 输入框（填空模式） | PrimaryContainer 淡底 + 无边框，聚焦时主色描边 |
| 题型 chip | 圆角 16dp，选中用 PrimaryContainer 底 + 主色文字 |
| 结果反馈条 | 语义色变淡背景 + 语义色文字，圆角 8dp |

---

## 8. 动效规范

| 场景 | 动效 | 时长 |
|------|------|------|
| 判分对/错 | 选项背景色切换 + 轻微缩放（scale 1.0→1.03→1.0） | 200ms |
| 换一题 | 题干区淡出→淡入（fade + 轻微位移） | 250ms |
| 结果出现 | 解析区从下方轻微上浮淡入 | 200ms |
| 主题切换 | 颜色平滑过渡 | 300ms |

> 原则：不做撒花/粒子等庆祝动效，保持克制。

---

## 9. 无障碍与易用性

- 文字与背景对比度满足 WCAG AA（正文 ≥ 4.5:1，标题 ≥ 3:1）。
- 语义不只靠颜色传达（对错同时显示 ✓/✗ 图标 + 文字）。
- 点击目标区域 ≥ 48dp。

---

## 附：色值速查（实现时直接引用）

```kotlin
// 语义色
val Success = Color(0xFF52C41A)
val Error   = Color(0xFFF5222D)
val Warning = Color(0xFFFA8C16)

// 主色（建议值，待确认）
val PrimaryLight = Color(0xFF4E6EF2)
val PrimaryDark  = Color(0xFF6C8CFF)

// 浅色
val LightBackground   = Color(0xFFF5F7FA)
val LightSurface      = Color(0xFFFFFFFF)
val LightTextPrimary  = Color(0xFF1D2129)
val LightTextSecondary= Color(0xFF4E5969)
val LightTextTertiary = Color(0xFF86909C)
val LightDivider      = Color(0xFFE5E6EB)
val LightContainer    = Color(0xFFEAEEFE)  // 主色变淡

// 深色
val DarkBackground    = Color(0xFF17171A)
val DarkSurface       = Color(0xFF26262B)
val DarkTextPrimary   = Color(0xFFF5F5F7)
val DarkTextSecondary = Color(0xFFC9CDD4)
val DarkTextTertiary  = Color(0xFF86909C)
val DarkDivider       = Color(0xFF2E2E33)
val DarkContainer     = Color(0xFF2B3350)  // 主色变淡
```
