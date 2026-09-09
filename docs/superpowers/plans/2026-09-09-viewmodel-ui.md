# Quick Calculation — ViewModel + Compose UI 实现计划（M2 + M3）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已完成的 domain 出题引擎之上，实现 M2（QuizViewModel 状态管理）与 M3（Compose UI：题型选择页 + 刷题页 + 双主题），并 `assembleDebug` 打包通过。

**Architecture:** 单 Activity + Navigation-Compose（两个路由：`type-select` / `question`）。状态集中在一个 `QuizViewModel`（`StateFlow<QuizUiState>`），所有状态变更走同步方法（无内部协程），使其可 JVM 单测。主题三档（浅色/深色/跟随系统）经 DataStore 持久化，由 `ThemeRepository` 读写。UI 层只做展示与事件转发，判分逻辑复用 domain 的 `AnswerJudge`。

**Tech Stack:** Kotlin 2.4.20、Jetpack Compose（Material3）、navigation-compose 2.10.0、datastore-preferences 1.2.1、lifecycle-viewmodel-compose（已有）、JUnit 4。

**Spec:** `docs/UI_DESIGN.md`（配色/排版/组件/动效规范）、`docs/PLAN.md`（§6 架构、§7 交互、§8.4 统计模型）。已定稿决策：Navigation-Compose、DataStore 持久化主题、`Question` 用 PLAN 版模型（`topic: String`）。

## Global Constraints

- 包根 `com.example.quickcalculation`；UI 放 `ui/`，状态放 `viewmodel/`，主题放 `ui/theme/`。
- domain 层（M1 交付）**不可修改**，判分与出题只能复用：`QuestionGenerator(seed?)`、`AnswerJudge`、`QuestionType`、`Difficulty`、`Question`、`QuizStats`、`NumberUtil`。
- 答案展示规则（标度规范裁决，见 M1 ledger）：`Question.unit == "%"` / `"个百分点"` 的 `correctAnswer` 已是「百分点」（如 20.0 表示 20%），展示时 `format(value) + unit` 直接拼接即可，**不要再 ÷100**。其余 unit（亿元/万人/元/倍）为量级值，同样直接拼接。
- 配色、字号、圆角、间距严格取自 `UI_DESIGN.md`（见各 Task 的色值速查）。
- 语义色对错用绿/红并配 ✓/✗ 图标 + 文字（不只靠颜色）。
- 点击目标 ≥ 48dp。
- 测试用 JUnit 4；纯 Kotlin 可测部分（ViewModel、枚举）放 `app/src/test/`，运行 `./gradlew testDebugUnitTest`。

---

### Task 1: 依赖 + 主题系统（Color/Type/Theme + ThemeMode/AnswerMode 枚举）

**Files:**
- Modify: `gradle/libs.versions.toml`（加 navigationCompose/datastore 版本与 library）
- Modify: `app/build.gradle.kts`（加两个 `implementation`）
- Create: `app/src/main/java/com/example/quickcalculation/ui/theme/ThemeMode.kt`
- Create: `app/src/main/java/com/example/quickcalculation/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/quickcalculation/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/quickcalculation/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/example/quickcalculation/viewmodel/AnswerMode.kt`
- Test: `app/src/test/java/com/example/quickcalculation/viewmodel/UiEnumTest.kt`

**Interfaces:**
- Produces:
  - `enum class ThemeMode { LIGHT, DARK, SYSTEM }`（包 `com.example.quickcalculation.ui.theme`）
  - `enum class AnswerMode { CHOICE, FILL }`（包 `com.example.quickcalculation.viewmodel`）
  - `object QuickColors`（语义色 + 主色 + 深浅色板常量，见 UI_DESIGN 速查）
  - `@Composable fun QuickCalculationTheme(themeMode: ThemeMode, content)`（内部 `isDark` 判定并切换 `ColorScheme`）

- [ ] **Step 1: 写失败测试**

`app/src/test/java/com/example/quickcalculation/viewmodel/UiEnumTest.kt`:

```kotlin
package com.example.quickcalculation.viewmodel

import com.example.quickcalculation.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class UiEnumTest {
    @Test
    fun themeMode_hasThreeValues() {
        assertEquals(3, ThemeMode.entries.size)
    }

    @Test
    fun answerMode_hasTwoValues() {
        assertEquals(2, AnswerMode.entries.size)
        assertEquals(AnswerMode.CHOICE, AnswerMode.valueOf("CHOICE"))
        assertEquals(AnswerMode.FILL, AnswerMode.valueOf("FILL"))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.viewmodel.UiEnumTest"`
Expected: 编译失败（`ThemeMode`/`AnswerMode` 不存在）。

- [ ] **Step 3: 加依赖**

`gradle/libs.versions.toml` `[versions]` 增：
```toml
navigationCompose = "2.10.0"
datastore = "1.2.1"
```
`[libraries]` 增：
```toml
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```
`app/build.gradle.kts` `dependencies` 增：
```kotlin
implementation(libs.androidx.navigation.compose)
implementation(libs.androidx.datastore.preferences)
```

- [ ] **Step 4: 写实现（枚举 + 主题）**

`ThemeMode.kt`:

```kotlin
package com.example.quickcalculation.ui.theme

enum class ThemeMode { LIGHT, DARK, SYSTEM }
```

`AnswerMode.kt`:

```kotlin
package com.example.quickcalculation.viewmodel

enum class AnswerMode { CHOICE, FILL }
```

`Color.kt`（取自 UI_DESIGN 附色值速查，逐字抄录）:

```kotlin
package com.example.quickcalculation.ui.theme

import androidx.compose.ui.graphics.Color

object QuickColors {
    // 语义色
    val Success = Color(0xFF52C41A)
    val Error = Color(0xFFF5222D)
    val Warning = Color(0xFFFA8C16)

    // 主色
    val PrimaryLight = Color(0xFF4E6EF2)
    val PrimaryDark = Color(0xFF6C8CFF)

    // 浅色
    val LightBackground = Color(0xFFF5F7FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightTextPrimary = Color(0xFF1D2129)
    val LightTextSecondary = Color(0xFF4E5969)
    val LightTextTertiary = Color(0xFF86909C)
    val LightDivider = Color(0xFFE5E6EB)
    val LightContainer = Color(0xFFEAEEFE)

    // 深色
    val DarkBackground = Color(0xFF17171A)
    val DarkSurface = Color(0xFF26262B)
    val DarkTextPrimary = Color(0xFFF5F5F7)
    val DarkTextSecondary = Color(0xFFC9CDD4)
    val DarkTextTertiary = Color(0xFF86909C)
    val DarkDivider = Color(0xFF2E2E33)
    val DarkContainer = Color(0xFF2B3350)
}
```

`Type.kt`:

```kotlin
package com.example.quickcalculation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val QuickTypography = Typography(
    displayLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
)
```

`Theme.kt`:

```kotlin
package com.example.quickcalculation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = QuickColors.PrimaryLight,
    primaryContainer = QuickColors.LightContainer,
    background = QuickColors.LightBackground,
    surface = QuickColors.LightSurface,
    onPrimary = QuickColors.LightSurface,
    onBackground = QuickColors.LightTextPrimary,
    onSurface = QuickColors.LightTextPrimary,
    onSurfaceVariant = QuickColors.LightTextSecondary,
    outline = QuickColors.LightDivider,
    error = QuickColors.Error,
)

private val DarkScheme = darkColorScheme(
    primary = QuickColors.PrimaryDark,
    primaryContainer = QuickColors.DarkContainer,
    background = QuickColors.DarkBackground,
    surface = QuickColors.DarkSurface,
    onPrimary = QuickColors.DarkBackground,
    onBackground = QuickColors.DarkTextPrimary,
    onSurface = QuickColors.DarkTextPrimary,
    onSurfaceVariant = QuickColors.DarkTextSecondary,
    outline = QuickColors.DarkDivider,
    error = QuickColors.Error,
)

@Composable
fun QuickCalculationTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (isDark) DarkScheme else LightScheme,
        typography = QuickTypography,
        content = content,
    )
}
```

- [ ] **Step 5: 运行确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.viewmodel.UiEnumTest"`
Expected: PASS。

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat(ui): add theme system, ThemeMode/AnswerMode enums, navigation+datastore deps"
```

---

### Task 2: QuizViewModel 状态管理（纯 Kotlin，可 JVM 单测）

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/viewmodel/QuizUiState.kt`
- Create: `app/src/main/java/com/example/quickcalculation/viewmodel/QuizViewModel.kt`
- Test: `app/src/test/java/com/example/quickcalculation/viewmodel/QuizViewModelTest.kt`

**Interfaces:**
- Consumes: `QuestionGenerator`, `QuestionType`, `Difficulty`, `Question`, `QuizStats`, `AnswerJudge`, `NumberUtil`（domain 层）。
- Produces:
  - `data class QuizUiState(selectedTypes: Set<QuestionType> = emptySet(), difficulty: Difficulty = Difficulty.EASY, answerMode: AnswerMode = AnswerMode.CHOICE, question: Question? = null, selectedOption: Int? = null, fillInput: String = "", answered: Boolean = false, isCorrect: Boolean? = null, stats: QuizStats = QuizStats())`
  - `class QuizViewModel(private val generator: QuestionGenerator = QuestionGenerator()) : ViewModel()`，方法：
    - `val state: StateFlow<QuizUiState>`
    - `fun toggleType(type: QuestionType)`
    - `fun setDifficulty(d: Difficulty)`
    - `fun setAnswerMode(m: AnswerMode)`
    - `fun startQuiz()`（生成首题，重置 stats）
    - `fun nextQuestion()`（生成下一题，清作答态）
    - `fun selectOption(index: Int)`（选择模式判分，标记 answered/isCorrect，累加 stats）
    - `fun submitFill(text: String)`（填空模式：解析 Double → `AnswerJudge.judgeFill` → 判分，累加 stats；解析失败不判分，仅回显 input）
    - `fun setFillInput(text: String)`

- [ ] **Step 1: 写失败测试**

`QuizViewModelTest.kt`:

```kotlin
package com.example.quickcalculation.viewmodel

import com.example.quickcalculation.domain.generator.QuestionGenerator
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizViewModelTest {

    private fun vm() = QuizViewModel(QuestionGenerator(seed = 42))

    @Test
    fun startQuiz_generatesQuestionAndResetsStats() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.startQuiz()
        val s = vm.state.value
        assertNotNull(s.question)
        assertEquals(0, s.stats.answered)
        assertEquals(QuestionType.BASE_PERIOD, s.question!!.type)
    }

    @Test
    fun toggleType_addsAndRemoves() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.toggleType(QuestionType.RATIO)
        assertTrue(vm.state.value.selectedTypes.containsAll(listOf(QuestionType.BASE_PERIOD, QuestionType.RATIO)))
        vm.toggleType(QuestionType.BASE_PERIOD)
        assertFalse(QuestionType.BASE_PERIOD in vm.state.value.selectedTypes)
    }

    @Test
    fun selectOption_correctAnswer_marksCorrectAndIncrementsStats() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.startQuiz()
        val correctIdx = vm.state.value.question!!.options.indexOf(vm.state.value.question!!.correctAnswer)
        vm.selectOption(correctIdx)
        val s = vm.state.value
        assertTrue(s.answered)
        assertEquals(true, s.isCorrect)
        assertEquals(1, s.stats.answered)
        assertEquals(1, s.stats.correct)
    }

    @Test
    fun selectOption_wrongAnswer_marksIncorrect() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.startQuiz()
        val q = vm.state.value.question!!
        val wrongIdx = q.options.indices.first { q.options[it] != q.correctAnswer }
        vm.selectOption(wrongIdx)
        val s = vm.state.value
        assertTrue(s.answered)
        assertEquals(false, s.isCorrect)
        assertEquals(1, s.stats.answered)
        assertEquals(0, s.stats.correct)
    }

    @Test
    fun submitFill_withinTolerance_marksCorrect() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.setAnswerMode(AnswerMode.FILL)
        vm.startQuiz()
        val correct = vm.state.value.question!!.correctAnswer
        vm.submitFill((correct * 1.001).toString())
        val s = vm.state.value
        assertTrue(s.answered)
        assertEquals(true, s.isCorrect)
    }

    @Test
    fun submitFill_invalidInput_doesNotJudge() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.setAnswerMode(AnswerMode.FILL)
        vm.startQuiz()
        vm.submitFill("not a number")
        val s = vm.state.value
        assertFalse(s.answered)
        assertEquals("not a number", s.fillInput)
    }

    @Test
    fun nextQuestion_clearsAnswerState() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.startQuiz()
        val correctIdx = vm.state.value.question!!.options.indexOf(vm.state.value.question!!.correctAnswer)
        vm.selectOption(correctIdx)
        vm.nextQuestion()
        val s = vm.state.value
        assertFalse(s.answered)
        assertNull(s.isCorrect)
        assertEquals(1, s.stats.answered) // stats 保留
    }

    @Test
    fun startQuiz_withoutTypes_doesNothing() {
        val vm = vm()
        vm.startQuiz()
        assertNull(vm.state.value.question)
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.viewmodel.QuizViewModelTest"`
Expected: 编译失败（`QuizViewModel`/`QuizUiState` 不存在）。

- [ ] **Step 3: 写实现**

`QuizUiState.kt`:

```kotlin
package com.example.quickcalculation.viewmodel

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.model.QuizStats

data class QuizUiState(
    val selectedTypes: Set<QuestionType> = emptySet(),
    val difficulty: Difficulty = Difficulty.EASY,
    val answerMode: AnswerMode = AnswerMode.CHOICE,
    val question: Question? = null,
    val selectedOption: Int? = null,
    val fillInput: String = "",
    val answered: Boolean = false,
    val isCorrect: Boolean? = null,
    val stats: QuizStats = QuizStats(),
)
```

`QuizViewModel.kt`:

```kotlin
package com.example.quickcalculation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.quickcalculation.domain.generator.QuestionGenerator
import com.example.quickcalculation.domain.judge.AnswerJudge
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.model.QuizStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QuizViewModel(
    private val generator: QuestionGenerator = QuestionGenerator(),
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    fun toggleType(type: QuestionType) = _state.update { s ->
        val set = if (type in s.selectedTypes) s.selectedTypes - type else s.selectedTypes + type
        s.copy(selectedTypes = set)
    }

    fun setDifficulty(d: Difficulty) = _state.update { it.copy(difficulty = d) }

    fun setAnswerMode(m: AnswerMode) = _state.update { it.copy(answerMode = m) }

    fun startQuiz() = _state.update { s ->
        if (s.selectedTypes.isEmpty()) return@update s
        s.copy(question = generator.generateRandom(s.selectedTypes.toList(), s.difficulty), stats = QuizStats())
    }

    fun nextQuestion() = _state.update { s ->
        val q = s.question ?: return@update s
        s.copy(question = generator.generateRandom(s.selectedTypes.toList(), s.difficulty),
            selectedOption = null, fillInput = "", answered = false, isCorrect = null)
    }

    fun selectOption(index: Int) = _state.update { s ->
        val q = s.question ?: return@update s
        if (s.answered || index !in q.options.indices) return@update s
        val correct = AnswerJudge.judgeChoice(q, index)
        s.copy(selectedOption = index, answered = true, isCorrect = correct,
            stats = s.stats.copy(answered = s.stats.answered + 1, correct = s.stats.correct + if (correct) 1 else 0))
    }

    fun setFillInput(text: String) = _state.update { it.copy(fillInput = text) }

    fun submitFill(text: String) = _state.update { s ->
        val q = s.question ?: return@update s
        if (s.answered) return@update s
        val value = text.toDoubleOrNull() ?: return@update s.copy(fillInput = text)
        val tolerance = AnswerJudge.defaultTolerance(s.difficulty)
        val correct = AnswerJudge.judgeFill(q, value, tolerance)
        s.copy(fillInput = text, answered = true, isCorrect = correct,
            stats = s.stats.copy(answered = s.stats.answered + 1, correct = s.stats.correct + if (correct) 1 else 0))
    }
}
```

> 注：`androidx.lifecycle.ViewModel` 来自已有的 `lifecycle-viewmodel-compose` 依赖；`kotlinx.coroutines.flow` 来自该依赖的传递依赖。若测试环境缺少 coroutines-flow（JVM 单测），在 `app/build.gradle.kts` 的 `testImplementation` 增 `testImplementation(libs.kotlinx.coroutines.test)` 不必要——`MutableStateFlow` 在 `lifecycle-viewmodel` 的传递依赖里已有，直接可用。若编译报 coroutines 缺失，改加 `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")` 由实现者判断。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.viewmodel.QuizViewModelTest"`
Expected: PASS（8 个测试全绿）。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat(viewmodel): add QuizViewModel with StateFlow state and unit tests"
```

---

### Task 3: ThemeRepository（DataStore）+ MainActivity + Navigation 骨架

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/data/ThemeRepository.kt`
- Create: `app/src/main/java/com/example/quickcalculation/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`（声明 MainActivity + `android:exported="true"` + launcher intent-filter）

**Interfaces:**
- Consumes: `ThemeMode`（Task 1）。
- Produces:
  - `class ThemeRepository(private val context: Context)`，`val themeMode: Flow<ThemeMode>`，`suspend fun setThemeMode(mode: ThemeMode)`
  - `MainActivity`（`ComponentActivity`，`setContent { ... }`，内部用 `rememberNavController()` 建 NavHost，路由 `type-select` 与 `question`，主题经 `ThemeRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)` 注入 `QuickCalculationTheme`）

- [ ] **Step 1: 写实现（无独立单测——DataStore 依赖 Android Context，留 M4 联调验证）**

`ThemeRepository.kt`:

```kotlin
package com.example.quickcalculation.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.quickcalculation.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeRepository(private val context: Context) {

    private val key = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[key] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[key] = mode.name }
    }
}
```

`MainActivity.kt`（骨架，路由内容在 Task 4/5 补）:

```kotlin
package com.example.quickcalculation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.quickcalculation.data.ThemeRepository
import com.example.quickcalculation.ui.theme.QuickCalculationTheme
import com.example.quickcalculation.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeRepo = ThemeRepository(applicationContext)
        setContent {
            val themeMode by themeRepo.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            QuickCalculationTheme(themeMode = themeMode) {
                // NavHost 在 Task 4 补齐；此处先放占位
            }
        }
    }
}
```

`AndroidManifest.xml` 在 `<application>` 内、`</application>` 前增：

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.QuickCalculation">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

- [ ] **Step 2: 编译验证（无单测，先保证 compileDebugKotlin 通过）**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL。若 DataStore 依赖下载/编译失败，检查 `libs.versions.toml` 键名与 `build.gradle.kts` 引用一致。

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat(ui): add ThemeRepository (DataStore), MainActivity skeleton, manifest launcher"
```

---

### Task 4: TypeSelectScreen（题型选择页）

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/ui/TypeSelectScreen.kt`
- Create: `app/src/main/java/com/example/quickcalculation/ui/AnswerFormatter.kt`
- Modify: `app/src/main/java/com/example/quickcalculation/MainActivity.kt`（补 NavHost + 路由到两个 Screen）

**Interfaces:**
- Consumes: `QuizViewModel`（Task 2）、`ThemeMode`/`ThemeRepository`（Task 1/3）、`QuestionType`、`Difficulty`、`AnswerMode`。
- Produces:
  - `@Composable fun TypeSelectScreen(vm: QuizViewModel, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, onStart: () -> Unit)`
  - `object AnswerFormatter { fun format(value: Double, unit: String): String }`（`NumberUtil.format(value)` 去尾零 + 拼 unit；`unit == "%"` 时拼 `%`，`"个百分点"` 拼 `个百分点`，`"倍"` 拼 `倍`，其余量级单位直接拼）

- [ ] **Step 1: 写实现**

`AnswerFormatter.kt`:

```kotlin
package com.example.quickcalculation.ui

import com.example.quickcalculation.domain.util.NumberUtil

object AnswerFormatter {
    fun format(value: Double, unit: String): String =
        NumberUtil.format(value, decimals = if (value % 1.0 == 0.0) 0 else 2) + unit
}
```

`TypeSelectScreen.kt`（要点：标题、题型多选 chip 列表、难度三档、作答模式切换、主题三档、开始按钮；chip 圆角 16dp、主色选中态；点击目标 ≥ 48dp）：

```kotlin
package com.example.quickcalculation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.ui.theme.ThemeMode
import com.example.quickcalculation.viewmodel.AnswerMode
import com.example.quickcalculation.viewmodel.QuizViewModel

@Composable
fun TypeSelectScreen(
    vm: QuizViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onStart: () -> Unit,
) {
    val state = vm.state
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text("选择题型", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        QuestionType.entries.forEach { type ->
            FilterChip(
                selected = type in state.value.selectedTypes,
                onClick = { vm.toggleType(type) },
                label = { Text(type.label) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("难度", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(selected = state.value.difficulty == d, onClick = { vm.setDifficulty(d) },
                    label = { Text(difficultyLabel(d)) })
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("作答模式", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.value.answerMode == AnswerMode.CHOICE,
                onClick = { vm.setAnswerMode(AnswerMode.CHOICE) }, label = { Text("选择") })
            FilterChip(selected = state.value.answerMode == AnswerMode.FILL,
                onClick = { vm.setAnswerMode(AnswerMode.FILL) }, label = { Text("填空") })
        }
        Spacer(Modifier.height(24.dp))
        Text("主题", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(selected = themeMode == m, onClick = { onThemeModeChange(m) },
                    label = { Text(themeModeLabel(m)) })
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { vm.startQuiz(); onStart() },
            enabled = state.value.selectedTypes.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("开始练习") }
    }
}

private fun difficultyLabel(d: Difficulty) = when (d) {
    Difficulty.EASY -> "简单"
    Difficulty.MEDIUM -> "中等"
    Difficulty.HARD -> "困难"
}
private fun themeModeLabel(m: ThemeMode) = when (m) {
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
    ThemeMode.SYSTEM -> "跟随系统"
}
```

`MainActivity.kt` 补 NavHost（替换占位）：

```kotlin
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quickcalculation.ui.AnswerFormatter
import com.example.quickcalculation.ui.TypeSelectScreen
import com.example.quickcalculation.ui.QuestionScreen
import com.example.quickcalculation.viewmodel.QuizViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// setContent 内：
val themeMode by themeRepo.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
QuickCalculationTheme(themeMode = themeMode) {
    val vm: QuizViewModel = viewModel()
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    NavHost(navController = nav, startDestination = "type-select") {
        composable("type-select") {
            TypeSelectScreen(vm = vm, themeMode = themeMode,
                onThemeModeChange = { m -> scope.launch { themeRepo.setThemeMode(m) } },
                onStart = { nav.navigate("question") })
        }
        composable("question") {
            QuestionScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL（`QuestionScreen` 在 Task 5 实现前会编译失败——本 Task 先建 `QuestionScreen` 空壳签名以通过编译）。

在 `app/src/main/java/com/example/quickcalculation/ui/QuestionScreen.kt` 先建空壳：

```kotlin
package com.example.quickcalculation.ui

import androidx.compose.runtime.Composable
import com.example.quickcalculation.viewmodel.QuizViewModel

@Composable
fun QuestionScreen(vm: QuizViewModel, onBack: () -> Unit) {
    // Task 5 实现
}
```

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat(ui): add TypeSelectScreen, AnswerFormatter, navigation wiring"
```

---

### Task 5: QuestionScreen（刷题页）

**Files:**
- Modify: `app/src/main/java/com/example/quickcalculation/ui/QuestionScreen.kt`（实现完整刷题页）

**Interfaces:**
- Consumes: `QuizViewModel`、`AnswerFormatter`、`AnswerMode`、`QuestionType`、`Difficulty`。
- Produces: 完整 `QuestionScreen(vm, onBack)`：顶部统计条（已答 N · 正确率 X%）、题型 chip + 作答模式、题干区、作答区（选择：4 选项按钮；填空：数字输入框）、结果区（✓/✗ + 正确答案 + 解析）、底部「换一题/下一题」。

- [ ] **Step 1: 写实现**

`QuestionScreen.kt`（要点：选择模式选项点击立即判分，正确项绿底/错误项红底 + ✓/✗；填空确认后判分；结果区显示 `AnswerFormatter.format(correctAnswer, unit)` 与 `explanation`；对错用语义色 + 图标文字；正确率 = `stats.accuracy`）：

```kotlin
package com.example.quickcalculation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.ui.theme.QuickColors
import com.example.quickcalculation.viewmodel.AnswerMode
import com.example.quickcalculation.viewmodel.QuizViewModel

@Composable
fun QuestionScreen(vm: QuizViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycleCompat()
    val q = state.question
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("返回") }
            Text("已答 ${state.stats.answered} · 正确率 ${accuracyText(state.stats.accuracy)}",
                style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        if (q != null) {
            Text("${q.type.label} · ${q.subType}", style = MaterialTheme.typography.labelLarge,
                color = QuickColors.PrimaryLight)
            Spacer(Modifier.height(12.dp))
            Text(q.stem, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            if (state.answerMode == AnswerMode.CHOICE) {
                q.options.forEachIndexed { idx, opt ->
                    val isSelected = state.selectedOption == idx
                    val isCorrectOption = opt == q.correctAnswer
                    val showGreen = state.answered && isCorrectOption
                    val showRed = state.answered && isSelected && !isCorrectOption
                    OptionButton(
                        text = AnswerFormatter.format(opt, q.unit),
                        selected = isSelected,
                        green = showGreen,
                        red = showRed,
                        onClick = { vm.selectOption(idx) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                OutlinedTextField(
                    value = state.fillInput,
                    onValueChange = { vm.setFillInput(it) },
                    label = { Text("输入答案（${q.unit}）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.submitFill(state.fillInput) }, enabled = !state.answered) {
                    Text("确认")
                }
            }
            if (state.answered) {
                Spacer(Modifier.height(16.dp))
                ResultBar(correct = state.isCorrect == true, answer = AnswerFormatter.format(q.correctAnswer, q.unit), explanation = q.explanation)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.nextQuestion() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("下一题")
            }
        }
    }
}
```

辅助 Composable（同文件）：`OptionButton`（默认白底/分割线描边、选中语义色描边+变淡填充、对错绿/红）、`ResultBar`（语义色变淡背景 + 语义色文字，✓/✗ + 正确答案 + 解析）、`accuracyText`（`"${(a * 100).toInt()}%"`）。

> 注：`collectAsStateWithLifecycleCompat` 需要 `lifecycle-runtime-compose`。若无该依赖，直接 `vm.state.collectAsState()` 替代（`androidx.compose.runtime.collectAsState`，Compose 自带）。为避免引入新依赖，实现者用 `collectAsState()`。

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat(ui): implement QuestionScreen with choice/fill answer, judging, result display"
```

---

### Task 6: 联调 + 打包验证

**Files:** 无新增（可能微调 import/空壳清理）。

- [ ] **Step 1: 全量单测**

Run: `./gradlew testDebugUnitTest`
Expected: 全部 PASS（M1 的 47 个 + M2 的 9 个）。

- [ ] **Step 2: 打包**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL，产出 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 3: 提交（如有清理）**

```bash
git add -A && git commit -m "chore: finalize M2-M3 integration" || echo "nothing to commit"
```

---

## 完成标准（M2 + M3 Done Definition）

- [ ] `./gradlew testDebugUnitTest` 全绿（domain 47 + viewmodel 9）。
- [ ] `./gradlew assembleDebug` 产出 `app-debug.apk`。
- [ ] 题型选择页：多选 chip、难度三档、作答模式、主题三档、开始按钮（无选题禁用）。
- [ ] 刷题页：选择/填空双模式、判分、✓/✗ 对错反馈、正确答案 + 解析、统计、下一题。
- [ ] 主题三档可切换且经 DataStore 持久化。
- [ ] 配色/排版/圆角符合 UI_DESIGN.md。

**后续（不在本计划内）**：M4 真机联调、M5 优化（ledger 里的 M5 backlog 一并处理）。
