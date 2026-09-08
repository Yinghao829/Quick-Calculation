# Quick Calculation — Domain 出题引擎实现计划（M0 + M1）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现纯 Kotlin 的 `domain` 出题引擎层：7 大类题型生成器 + 判分器 + 题材库 + 工具类，全部通过 JVM 单元测试。

**Architecture:** 单模块 `app`，业务逻辑全部放在不依赖 Android 的 `domain/` 包下（可 JVM 单测）。生成器按题型拆成独立 `object`，通过 `QuestionGenerator` 统一分发。答案在生成时计算好，判分只做比较。所有生成器接收 `Random`（由 `QuestionGenerator` 的 seed 控制），保证可复现。

**Tech Stack:** Kotlin 2.4.20、JUnit 4.13.2（已配置，见 `gradle/libs.versions.toml`）。

**Spec:** 设计文档位于 `docs/`（`PLAN.md`、`QUESTION_GENERATION_ENGINE.md`、`UI_DESIGN.md`）。本计划实现 PLAN 的 M0（骨架）+ M1（出题器核心）。关键已定稿决策：
- 出题架构：**独立出题**（无 DataContext 联动）
- 题材库：PLAN 的 8 题材（`Topic` 模型）
- 难度枚举：`EASY / MEDIUM / HARD`
- 增长率：按难度加权采样池
- `Question` 模型用 PLAN 版（`topic: String`，无 `dataContext`）

## Global Constraints

- 包根：`com.example.quickcalculation`，业务代码放 `domain/`、`viewmodel/`、`ui/`、`util/` 子包。
- `domain/` 内**禁止 import 任何 `android.*`**，保证可 JVM 单测。
- 数学不变量（贯穿所有生成器，测试强制）：`A = B×(1+r)`、`X = A−B`、`r = X÷B`；比重 `0 < P < Q`；倍数 `A ≥ B`。
- 增长率 `r` 必须落在题材 `growthRange` 内（R5 硬约束）。
- 选择题 4 选项：互不重复、量级一致、唯一正确。
- 填空判分相对容差：EASY/MEDIUM `0.005`，HARD `0.002`。
- 所有生成器接受 `kotlin.random.Random` 入参；`Random(seed)` 复现同一道题。
- 测试用 JUnit 4，运行命令：`./gradlew testDebugUnitTest`。

---

### Task 1: 领域模型 + git 初始化

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/model/Difficulty.kt`
- Create: `app/src/main/java/com/example/quickcalculation/domain/model/QuestionType.kt`
- Create: `app/src/main/java/com/example/quickcalculation/domain/model/Topic.kt`
- Create: `app/src/main/java/com/example/quickcalculation/domain/model/Question.kt`
- Create: `app/src/main/java/com/example/quickcalculation/domain/model/QuizStats.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/model/ModelTest.kt`

**Interfaces:**
- Produces（后续任务依赖）:
  - `enum class Difficulty { EASY, MEDIUM, HARD }`
  - `enum class QuestionType(val label: String)` — 7 个值：`BASE_PERIOD("基期与现期")`、`GROWTH_AMOUNT("增长量")`、`GROWTH_RATE("一般增长率")`、`RATIO("比重")`、`AVERAGE("平均数")`、`MULTIPLE("倍数")`、`SPECIAL_RATE("特殊增长率")`
  - `data class Topic(name, unit, magnitude: ClosedRange<Double>, growthRange: ClosedRange<Double>, isRateType: Boolean = false)`
  - `data class Question(type, subType, topic, stem, correctAnswer, options: List<Double>, unit, explanation, difficulty)`
  - `data class QuizStats(answered=0, correct=0)` with `val accuracy: Float`

- [ ] **Step 1: 初始化 git 仓库**

```bash
cd /home/hao/AndroidStudioProjects/QuickCalculation
git init
git add -A
git commit -m "chore: initial project skeleton"
```

- [ ] **Step 2: 写失败测试**

`app/src/test/java/com/example/quickcalculation/domain/model/ModelTest.kt`:

```kotlin
package com.example.quickcalculation.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {
    @Test
    fun questionType_hasSevenTypesWithChineseLabels() {
        assertEquals(7, QuestionType.entries.size)
        assertEquals("基期与现期", QuestionType.BASE_PERIOD.label)
        assertEquals("特殊增长率", QuestionType.SPECIAL_RATE.label)
    }

    @Test
    fun quizStats_accuracy_zeroWhenUnanswered() {
        assertEquals(0f, QuizStats().accuracy)
    }

    @Test
    fun quizStats_accuracy_isCorrectOverAnswered() {
        assertEquals(0.6f, QuizStats(answered = 5, correct = 3).accuracy)
    }

    @Test
    fun topic_hasExpectedFields() {
        val t = Topic("国内生产总值", "亿元", 80000.0..150000.0, 0.02..0.08)
        assertEquals("亿元", t.unit)
        assertTrue(120000.0 in t.magnitude)
        assertTrue(0.05 in t.growthRange)
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.model.ModelTest"`
Expected: 编译失败（找不到 `Difficulty`、`QuestionType`、`Topic`、`QuizStats` 等类）。

- [ ] **Step 4: 写实现**

`Difficulty.kt`:

```kotlin
package com.example.quickcalculation.domain.model

enum class Difficulty { EASY, MEDIUM, HARD }
```

`QuestionType.kt`:

```kotlin
package com.example.quickcalculation.domain.model

enum class QuestionType(val label: String) {
    BASE_PERIOD("基期与现期"),
    GROWTH_AMOUNT("增长量"),
    GROWTH_RATE("一般增长率"),
    RATIO("比重"),
    AVERAGE("平均数"),
    MULTIPLE("倍数"),
    SPECIAL_RATE("特殊增长率"),
}
```

`Topic.kt`:

```kotlin
package com.example.quickcalculation.domain.model

/**
 * 题材定义。`isRateType` 为 true 表示该题材的「值」本身就是一个增速（如 CPI、工业增加值增速），
 * 仅用于增速类题目；数值类题目通过 [TopicRepository.randomValueTopic] 过滤掉。
 */
data class Topic(
    val name: String,
    val unit: String,
    val magnitude: ClosedRange<Double>,
    val growthRange: ClosedRange<Double>,
    val isRateType: Boolean = false,
)
```

`Question.kt`:

```kotlin
package com.example.quickcalculation.domain.model

data class Question(
    val type: QuestionType,
    val subType: String,
    val topic: String,
    val stem: String,
    val correctAnswer: Double,
    val options: List<Double>,
    val unit: String,
    val explanation: String,
    val difficulty: Difficulty,
)
```

`QuizStats.kt`:

```kotlin
package com.example.quickcalculation.domain.model

data class QuizStats(
    val answered: Int = 0,
    val correct: Int = 0,
) {
    val accuracy: Float get() = if (answered == 0) 0f else correct.toFloat() / answered
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.model.ModelTest"`
Expected: PASS（4 个测试全绿）。

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/model app/src/test/java/com/example/quickcalculation/domain/model
git commit -m "feat(domain): add core models (Difficulty, QuestionType, Topic, Question, QuizStats)"
```

---

### Task 2: NumberUtil 数值工具

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/util/NumberUtil.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/util/NumberUtilTest.kt`

**Interfaces:**
- Produces: `object NumberUtil` with
  - `fun round(value: Double, decimals: Int): Double`
  - `fun format(value: Double, decimals: Int = 2): String`
  - `fun formatPercent(rate: Double, decimals: Int = 1): String`（`0.125` → `"12.5%"`）
  - `fun withinTolerance(user: Double, correct: Double, tolerance: Double): Boolean`

- [ ] **Step 1: 写失败测试**

`NumberUtilTest.kt`:

```kotlin
package com.example.quickcalculation.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberUtilTest {
    @Test
    fun round_halfUp() {
        assertEquals(1.23, NumberUtil.round(1.234, 2), 0.0)
        assertEquals(1.24, NumberUtil.round(1.235, 2), 0.0)
        assertEquals(200.0, NumberUtil.round(200.0, 0), 0.0)
    }

    @Test
    fun format_stripsTrailingZeros() {
        assertEquals("20", NumberUtil.format(20.0, 1))
        assertEquals("12.5", NumberUtil.format(12.5, 1))
        assertEquals("0.5", NumberUtil.format(0.5, 1))
    }

    @Test
    fun formatPercent() {
        assertEquals("20%", NumberUtil.formatPercent(0.20))
        assertEquals("12.5%", NumberUtil.formatPercent(0.125))
        assertEquals("5%", NumberUtil.formatPercent(0.05))
    }

    @Test
    fun withinTolerance_relative() {
        assertTrue(NumberUtil.withinTolerance(100.4, 100.0, 0.005))
        assertFalse(NumberUtil.withinTolerance(101.0, 100.0, 0.005))
        assertTrue(NumberUtil.withinTolerance(0.0, 0.0, 0.005))
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.util.NumberUtilTest"`
Expected: 编译失败（`NumberUtil` 不存在）。

- [ ] **Step 3: 写实现**

`NumberUtil.kt`:

```kotlin
package com.example.quickcalculation.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object NumberUtil {

    fun round(value: Double, decimals: Int): Double =
        BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()

    fun format(value: Double, decimals: Int = 2): String =
        BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()

    fun formatPercent(rate: Double, decimals: Int = 1): String =
        "${format(rate * 100, decimals)}%"

    fun withinTolerance(user: Double, correct: Double, tolerance: Double): Boolean {
        if (correct == 0.0) return abs(user - correct) <= tolerance
        return abs(user - correct) / abs(correct) <= tolerance
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.util.NumberUtilTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/util app/src/test/java/com/example/quickcalculation/domain/util
git commit -m "feat(domain): add NumberUtil for rounding, formatting, tolerance"
```

---

### Task 3: 生成基础设施（GrowthRatePool / TopicRepository / GenerationUtil / BaseGrowth）

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/GrowthRatePool.kt`
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/TopicRepository.kt`
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/GenerationUtil.kt`
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/BaseGrowth.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/InfrastructureTest.kt`

**Interfaces:**
- Consumes: `Difficulty`、`Topic`、`NumberUtil`（Task 1、2）。
- Produces:
  - `object GrowthRatePool { fun sample(difficulty: Difficulty, random: Random): Double; fun poolFor(difficulty: Difficulty): List<Double> }`
  - `object TopicRepository { val topics: List<Topic>; fun randomValueTopic(random: Random): Topic }`
  - `object GenerationUtil { fun decimals(difficulty): Int; fun roundForDifficulty(value, difficulty): Double; fun sampleBase(topic, difficulty, random): Double; fun sampleRate(topic, difficulty, random): Double; fun year(random): Int }`
  - `data class BaseGrowth(topic, baseValue, growthRate)` with `currentValue`、`growthAmount` 计算属性

- [ ] **Step 1: 写失败测试**

`InfrastructureTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfrastructureTest {

    @Test
    fun growthRatePool_sampleAlwaysInPool() {
        val rnd = Random(42)
        repeat(200) {
            val r = GrowthRatePool.sample(Difficulty.EASY, rnd)
            assertTrue(r in GrowthRatePool.poolFor(Difficulty.EASY))
        }
    }

    @Test
    fun growthRatePool_hardIncludesNegative() {
        assertTrue(GrowthRatePool.poolFor(Difficulty.HARD).any { it < 0.0 })
    }

    @Test
    fun topicRepository_hasEightTopicsAndFiltersRateTypes() {
        assertEquals(8, TopicRepository.topics.size)
        assertEquals(8, TopicRepository.topics.map { it.name }.distinct().size)
        val rnd = Random(1)
        repeat(100) {
            assertTrue(!TopicRepository.randomValueTopic(rnd).isRateType)
        }
    }

    @Test
    fun baseGrowth_invariantsHold() {
        val topic = TopicRepository.topics.first()
        val bg = BaseGrowth(topic, baseValue = 500.0, growthRate = 0.20)
        assertEquals(600.0, bg.currentValue, 1e-9)
        assertEquals(100.0, bg.growthAmount, 1e-9)
        // r = X / B
        assertEquals(bg.growthRate, bg.growthAmount / bg.baseValue, 1e-9)
        // X = A * r / (1 + r)
        assertEquals(bg.growthAmount, bg.currentValue * bg.growthRate / (1 + bg.growthRate), 1e-9)
    }

    @Test
    fun sampleRate_staysInTopicGrowthRange() {
        val rnd = Random(7)
        val lowGrowth = Topic("常住人口", "万人", 500.0..3000.0, 0.0..0.02)
        repeat(100) {
            val r = GenerationUtil.sampleRate(lowGrowth, Difficulty.EASY, rnd)
            assertTrue(r in lowGrowth.growthRange)
        }
    }

    @Test
    fun sampleBase_positive() {
        val rnd = Random(3)
        val topic = TopicRepository.topics.first()
        repeat(100) {
            assertTrue(GenerationUtil.sampleBase(topic, Difficulty.MEDIUM, rnd) > 0.0)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.InfrastructureTest"`
Expected: 编译失败（`GrowthRatePool` 等不存在）。

- [ ] **Step 3: 写实现**

`GrowthRatePool.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import kotlin.random.Random

/** 增长率池：按难度加权采样，常见数值出现概率更高。每个条目为 (rate, weight)。 */
object GrowthRatePool {
    private val easy = listOf(
        0.05 to 3, 0.10 to 3, 0.20 to 2, 0.25 to 2,
    )
    private val medium = listOf(
        0.05 to 2, 0.08 to 2, 0.10 to 3, 0.125 to 2, 0.15 to 2, 0.20 to 3, 0.25 to 2,
    )
    private val hard = listOf(
        -0.20 to 1, -0.15 to 1, -0.125 to 1, -0.10 to 1, -0.08 to 1, -0.05 to 1,
        0.075 to 1, 0.125 to 2, 0.15 to 2, 0.175 to 1, 0.18 to 1, 0.20 to 2, 0.225 to 1, 0.25 to 2,
    )

    fun sample(difficulty: Difficulty, random: Random): Double {
        val pool = pool(difficulty)
        val total = pool.sumOf { it.second }
        var idx = random.nextInt(total)
        for ((rate, weight) in pool) {
            if (idx < weight) return rate
            idx -= weight
        }
        return pool.last().first
    }

    fun poolFor(difficulty: Difficulty): List<Double> = pool(difficulty).map { it.first }

    private fun pool(difficulty: Difficulty): List<Pair<Double, Int>> = when (difficulty) {
        Difficulty.EASY -> easy
        Difficulty.MEDIUM -> medium
        Difficulty.HARD -> hard
    }
}
```

`TopicRepository.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Topic
import kotlin.random.Random

object TopicRepository {
    val topics: List<Topic> = listOf(
        Topic("国内生产总值", "亿元", 80000.0..150000.0, 0.02..0.08),
        Topic("社会消费品零售总额", "亿元", 3000.0..60000.0, 0.03..0.10),
        Topic("进出口总额", "亿美元", 2000.0..30000.0, -0.10..0.15),
        Topic("规模以上工业增加值", "%", 3.0..9.0, 0.02..0.08, isRateType = true),
        Topic("固定资产投资", "亿元", 1000.0..50000.0, -0.05..0.10),
        Topic("居民人均可支配收入", "元", 20000.0..60000.0, 0.03..0.08),
        Topic("常住人口", "万人", 500.0..3000.0, 0.0..0.02),
        Topic("居民消费价格", "%", 0.0..3.0, 0.0..0.03, isRateType = true),
    )

    fun randomValueTopic(random: Random): Topic {
        val valueTopics = topics.filter { !it.isRateType }
        return valueTopics[random.nextInt(valueTopics.size)]
    }
}
```

`GenerationUtil.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Topic
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random
import kotlin.math.abs

object GenerationUtil {

    fun decimals(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 0
        Difficulty.MEDIUM -> 1
        Difficulty.HARD -> 2
    }

    fun roundForDifficulty(value: Double, difficulty: Difficulty): Double =
        NumberUtil.round(value, decimals(difficulty))

    fun sampleBase(topic: Topic, difficulty: Difficulty, random: Random): Double {
        val raw = random.nextDouble(topic.magnitude.start, topic.magnitude.endInclusive)
        var v = NumberUtil.round(raw, decimals(difficulty))
        if (difficulty == Difficulty.EASY) {
            v = NumberUtil.round(v / 10.0, 0) * 10.0 // 整十，便于手算
        }
        return if (v <= 0.0) topic.magnitude.start else v
    }

    fun sampleRate(topic: Topic, difficulty: Difficulty, random: Random): Double {
        // 优先从难度池取（保证可手算），但必须落在题材合理区间内（R5）。
        repeat(40) {
            val r = GrowthRatePool.sample(difficulty, random)
            if (r in topic.growthRange) return r
        }
        // 低增速题材（人口/CPI）：池内无满足值，改用 0.5% 步长的干净值。
        val lo = topic.growthRange.start
        val hi = topic.growthRange.endInclusive
        val step = 0.005
        val steps = ((hi - lo) / step).toInt().coerceAtLeast(1)
        val r = lo + random.nextInt(steps + 1) * step
        return r.coerceAtMost(hi)
    }

    fun year(random: Random): Int = 2020 + random.nextInt(6)
}
```

`BaseGrowth.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Topic

/**
 * 基期/增长关系原语：以干净基期 B 与增长率 r 为种子，派生现期 A、增长量 X，
 * 保证 A = B×(1+r)、X = A−B、r = X÷B 恒成立。
 */
data class BaseGrowth(
    val topic: Topic,
    val baseValue: Double,
    val growthRate: Double,
) {
    val currentValue: Double get() = baseValue * (1 + growthRate)
    val growthAmount: Double get() = currentValue - baseValue
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.InfrastructureTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator app/src/test/java/com/example/quickcalculation/domain/generator
git commit -m "feat(domain): add growth-rate pool, topic repository, generation utils, BaseGrowth primitive"
```

---

### Task 4: OptionGenerator 干扰项生成

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/OptionGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/OptionGeneratorTest.kt`

**Interfaces:**
- Consumes: `GenerationUtil`（Task 3）。
- Produces: `object OptionGenerator { fun build(correct: Double, difficulty: Difficulty, random: Random, extra: List<Double> = emptyList()): List<Double> }` — 返回 4 个乱序选项（含正确项）。

- [ ] **Step 1: 写失败测试**

`OptionGeneratorTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionGeneratorTest {
    @Test
    fun build_returnsFourDistinctOptionsWithCorrect() {
        val rnd = Random(9)
        repeat(500) {
            val correct = 200.0 + it
            val opts = OptionGenerator.build(correct, Difficulty.EASY, rnd)
            assertEquals(4, opts.size)
            assertEquals(4, opts.distinct().size)
            assertTrue(correct in opts)
        }
    }

    @Test
    fun build_positiveDistractorsForPositiveCorrect() {
        val rnd = Random(11)
        repeat(500) {
            val opts = OptionGenerator.build(120.0, Difficulty.MEDIUM, rnd)
            assertTrue(opts.all { it > 0.0 })
        }
    }

    @Test
    fun build_negativeCorrectAllowed() {
        val rnd = Random(13)
        val opts = OptionGenerator.build(-0.10, Difficulty.HARD, rnd)
        assertEquals(4, opts.distinct().size)
        assertTrue((-0.10) in opts)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.OptionGeneratorTest"`
Expected: 编译失败（`OptionGenerator` 不存在）。

- [ ] **Step 3: 写实现**

`OptionGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import kotlin.random.Random

object OptionGenerator {

    /** 生成 4 个乱序选项：1 个正确项 + 3 个干扰项（难度控制差距，量级接近、互不重复）。 */
    fun build(
        correct: Double,
        difficulty: Difficulty,
        random: Random,
        extra: List<Double> = emptyList(),
    ): List<Double> {
        val offsets = when (difficulty) {
            Difficulty.EASY -> listOf(-0.10, 0.10, -0.05, 0.05, 0.20, -0.20, 0.15, -0.15)
            Difficulty.MEDIUM -> listOf(-0.04, 0.04, -0.02, 0.02, -0.08, 0.08, -0.06, 0.06)
            Difficulty.HARD -> listOf(-0.01, 0.01, -0.005, 0.005, -0.02, 0.02, -0.015, 0.015)
        }
        val candidates = buildList {
            extra.forEach { add(GenerationUtil.roundForDifficulty(it, difficulty)) }
            offsets.forEach { add(GenerationUtil.roundForDifficulty(correct * (1 + it), difficulty)) }
        }
        var distractors = candidates
            .filter { it != correct }
            .filter { it > 0.0 || correct <= 0.0 }
            .distinct()

        var k = 2
        while (distractors.size < 3 && k < 30) {
            val up = GenerationUtil.roundForDifficulty(correct * (1 + 0.05 * k), difficulty)
            val down = GenerationUtil.roundForDifficulty(correct * (1 - 0.05 * k), difficulty)
            if (up != correct && (up > 0.0 || correct <= 0.0) && up !in distractors) distractors = distractors + up
            if (down != correct && (down > 0.0 || correct <= 0.0) && down !in distractors) distractors = distractors + down
            k++
        }

        return (distractors.shuffled(random).take(3) + correct).shuffled(random)
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.OptionGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/OptionGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/OptionGeneratorTest.kt
git commit -m "feat(domain): add OptionGenerator for choice-mode distractors"
```

---

### Task 5: BasePeriodGenerator 基期与现期

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/BasePeriodGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/BasePeriodGeneratorTest.kt`

**Interfaces:**
- Consumes: `Question`、`QuestionType`、`Difficulty`、`TopicRepository`、`GenerationUtil`、`BaseGrowth`、`OptionGenerator`、`NumberUtil`。
- Produces: `object BasePeriodGenerator { fun generate(difficulty: Difficulty, random: Random): Question }`，`subType` ∈ {"求基期量", "求现期量"}。

- [ ] **Step 1: 写失败测试**

`BasePeriodGeneratorTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BasePeriodGeneratorTest {
    @Test
    fun generate_isStructurallyValid() {
        val rnd = Random(1)
        repeat(300) { i ->
            val d = Difficulty.entries[i % 3]
            val q = BasePeriodGenerator.generate(d, rnd)
            assertEquals(QuestionType.BASE_PERIOD, q.type)
            assertTrue(q.subType in setOf("求基期量", "求现期量"))
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctAnswer in q.options)
            assertTrue(q.correctAnswer > 0.0)
            assertTrue(q.stem.contains(q.topic))
            assertTrue(q.explanation.isNotBlank())
        }
    }

    @Test
    fun generate_isDeterministicWithSeed() {
        val a = BasePeriodGenerator.generate(Difficulty.EASY, Random(123))
        val b = BasePeriodGenerator.generate(Difficulty.EASY, Random(123))
        assertEquals(a, b)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.BasePeriodGeneratorTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`BasePeriodGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object BasePeriodGenerator {

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val b = GenerationUtil.sampleBase(topic, difficulty, random)
        val r = GenerationUtil.sampleRate(topic, difficulty, random)
        val bg = BaseGrowth(topic, b, r)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        return if (random.nextBoolean()) {
            // 求基期量：已知现期 A 与增长率 r，求 B = A ÷ (1+r)
            val answer = GenerationUtil.roundForDifficulty(bg.baseValue, difficulty)
            val a = GenerationUtil.roundForDifficulty(bg.currentValue, difficulty)
            val stem = "${year}年${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，同比增长${NumberUtil.formatPercent(r)}，求基期${topic.name}。"
            val explanation = "基期量 = 现期量 ÷ (1 + 增长率) = ${NumberUtil.format(a, dec)} ÷ ${NumberUtil.format(1 + r, dec)} = ${NumberUtil.format(answer, dec)}${topic.unit}"
            Question(
                type = QuestionType.BASE_PERIOD, subType = "求基期量", topic = topic.name,
                stem = stem, correctAnswer = answer,
                options = OptionGenerator.build(answer, difficulty, random, listOf(a * (1 - r))),
                unit = topic.unit, explanation = explanation, difficulty = difficulty,
            )
        } else {
            // 求现期量：已知基期 B 与增长率 r，求 A = B × (1+r)
            val answer = GenerationUtil.roundForDifficulty(bg.currentValue, difficulty)
            val stem = "${year}年${topic.name}为${NumberUtil.format(b, dec)}${topic.unit}，同比增长${NumberUtil.formatPercent(r)}，求现期${topic.name}。"
            val explanation = "现期量 = 基期量 × (1 + 增长率) = ${NumberUtil.format(b, dec)} × ${NumberUtil.format(1 + r, dec)} = ${NumberUtil.format(answer, dec)}${topic.unit}"
            Question(
                type = QuestionType.BASE_PERIOD, subType = "求现期量", topic = topic.name,
                stem = stem, correctAnswer = answer,
                options = OptionGenerator.build(answer, difficulty, random, listOf(b * (1 + r / 2))),
                unit = topic.unit, explanation = explanation, difficulty = difficulty,
            )
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.BasePeriodGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/BasePeriodGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/BasePeriodGeneratorTest.kt
git commit -m "feat(domain): add BasePeriodGenerator"
```

---

### Task 6: GrowthAmountGenerator 增长量

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/GrowthAmountGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/GrowthAmountGeneratorTest.kt`

**Interfaces:**
- Consumes: 同 Task 5 基础设施。
- Produces: `object GrowthAmountGenerator { fun generate(difficulty: Difficulty, random: Random): Question }`，`subType` ∈ {"求增长量(现期+基期)", "求增长量(现期+增长率)"}。

- [ ] **Step 1: 写失败测试**

`GrowthAmountGeneratorTest.kt`（与 Task 5 测试结构一致，断言 `type == GROWTH_AMOUNT`、`subType` 取值、4 选项唯一正确、`correctAnswer > 0`、确定性）:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthAmountGeneratorTest {
    @Test
    fun generate_isStructurallyValid() {
        val rnd = Random(2)
        repeat(300) { i ->
            val d = Difficulty.entries[i % 3]
            val q = GrowthAmountGenerator.generate(d, rnd)
            assertEquals(QuestionType.GROWTH_AMOUNT, q.type)
            assertTrue(q.subType in setOf("求增长量(现期+基期)", "求增长量(现期+增长率)"))
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctAnswer in q.options)
            assertTrue(q.correctAnswer > 0.0)
        }
    }

    @Test
    fun generate_isDeterministicWithSeed() {
        val a = GrowthAmountGenerator.generate(Difficulty.MEDIUM, Random(55))
        val b = GrowthAmountGenerator.generate(Difficulty.MEDIUM, Random(55))
        assertEquals(a, b)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.GrowthAmountGeneratorTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`GrowthAmountGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object GrowthAmountGenerator {

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val b = GenerationUtil.sampleBase(topic, difficulty, random)
        val r = GenerationUtil.sampleRate(topic, difficulty, random)
        val bg = BaseGrowth(topic, b, r)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)
        val a = GenerationUtil.roundForDifficulty(bg.currentValue, difficulty)

        return if (random.nextBoolean()) {
            // X = A − B
            val answer = GenerationUtil.roundForDifficulty(bg.growthAmount, difficulty)
            val stem = "${year}年${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，上年为${NumberUtil.format(b, dec)}${topic.unit}，求增长量。"
            val explanation = "增长量 = 现期量 − 基期量 = ${NumberUtil.format(a, dec)} − ${NumberUtil.format(b, dec)} = ${NumberUtil.format(answer, dec)}${topic.unit}"
            Question(QuestionType.GROWTH_AMOUNT, "求增长量(现期+基期)", topic.name, stem, answer,
                OptionGenerator.build(answer, difficulty, random, listOf(a, b)), topic.unit, explanation, difficulty)
        } else {
            // X = A·r ÷ (1+r)
            val answer = GenerationUtil.roundForDifficulty(bg.currentValue * bg.growthRate / (1 + bg.growthRate), difficulty)
            val stem = "${year}年${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，同比增长${NumberUtil.formatPercent(r)}，求增长量。"
            val explanation = "增长量 = 现期量 × 增长率 ÷ (1 + 增长率) = ${NumberUtil.format(a, dec)} × ${NumberUtil.formatPercent(r)} ÷ ${NumberUtil.format(1 + r, dec)} = ${NumberUtil.format(answer, dec)}${topic.unit}"
            Question(QuestionType.GROWTH_AMOUNT, "求增长量(现期+增长率)", topic.name, stem, answer,
                OptionGenerator.build(answer, difficulty, random), topic.unit, explanation, difficulty)
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.GrowthAmountGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/GrowthAmountGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/GrowthAmountGeneratorTest.kt
git commit -m "feat(domain): add GrowthAmountGenerator"
```

---

### Task 7: GrowthRateGenerator 一般增长率

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/GrowthRateGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/GrowthRateGeneratorTest.kt`

**Interfaces:**
- Consumes: 基础设施。
- Produces: `object GrowthRateGenerator { fun generate(difficulty, random): Question }`，`subType` ∈ {"求增长率(现期+基期)", "求增长率(增长量+基期)"}，`unit == "%"`，`correctAnswer` 为小数形式（如 0.20）。

- [ ] **Step 1: 写失败测试**

`GrowthRateGeneratorTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthRateGeneratorTest {
    @Test
    fun generate_isStructurallyValid() {
        val rnd = Random(3)
        repeat(300) { i ->
            val d = Difficulty.entries[i % 3]
            val q = GrowthRateGenerator.generate(d, rnd)
            assertEquals(QuestionType.GROWTH_RATE, q.type)
            assertTrue(q.subType in setOf("求增长率(现期+基期)", "求增长率(增长量+基期)"))
            assertEquals("%", q.unit)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctAnswer in q.options)
        }
    }

    @Test
    fun generate_isDeterministicWithSeed() {
        val a = GrowthRateGenerator.generate(Difficulty.HARD, Random(9))
        val b = GrowthRateGenerator.generate(Difficulty.HARD, Random(9))
        assertEquals(a, b)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.GrowthRateGeneratorTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`GrowthRateGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object GrowthRateGenerator {

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val b = GenerationUtil.sampleBase(topic, difficulty, random)
        val r = GenerationUtil.sampleRate(topic, difficulty, random)
        val bg = BaseGrowth(topic, b, r)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)
        val a = GenerationUtil.roundForDifficulty(bg.currentValue, difficulty)
        val answer = GenerationUtil.roundForDifficulty(bg.growthRate, difficulty)

        return if (random.nextBoolean()) {
            // r = (A − B) ÷ B
            val stem = "${year}年${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，上年为${NumberUtil.format(b, dec)}${topic.unit}，求增长率。"
            val explanation = "增长率 = (现期量 − 基期量) ÷ 基期量 = (${NumberUtil.format(a, dec)} − ${NumberUtil.format(b, dec)}) ÷ ${NumberUtil.format(b, dec)} = ${NumberUtil.formatPercent(r)}"
            Question(QuestionType.GROWTH_RATE, "求增长率(现期+基期)", topic.name, stem, answer,
                OptionGenerator.build(answer, difficulty, random), "%", explanation, difficulty)
        } else {
            // r = X ÷ B
            val x = GenerationUtil.roundForDifficulty(bg.growthAmount, difficulty)
            val stem = "${year}年${topic.name}增长量为${NumberUtil.format(x, dec)}${topic.unit}，基期为${NumberUtil.format(b, dec)}${topic.unit}，求增长率。"
            val explanation = "增长率 = 增长量 ÷ 基期量 = ${NumberUtil.format(x, dec)} ÷ ${NumberUtil.format(b, dec)} = ${NumberUtil.formatPercent(r)}"
            Question(QuestionType.GROWTH_RATE, "求增长率(增长量+基期)", topic.name, stem, answer,
                OptionGenerator.build(answer, difficulty, random), "%", explanation, difficulty)
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.GrowthRateGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/GrowthRateGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/GrowthRateGeneratorTest.kt
git commit -m "feat(domain): add GrowthRateGenerator"
```

---

### Task 8: RatioGenerator 比重

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/RatioGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/RatioGeneratorTest.kt`

**Interfaces:**
- Consumes: 基础设施。
- Produces: `object RatioGenerator`，暴露纯函数 `presentRatio(p,q)`、`baseRatio(p,q,a,b)`、`gapRatio(p,q,a,b)`；`generate(difficulty, random): Question`，`subType` ∈ {"现期比重", "基期比重", "两期比重差"}。

- [ ] **Step 1: 写失败测试**

`RatioGeneratorTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RatioGeneratorTest {
    @Test
    fun presentRatio_formula() {
        assertEquals(0.35, RatioGenerator.presentRatio(35.0, 100.0), 1e-9)
    }

    @Test
    fun baseRatio_formula() {
        // (P/Q) * (1+b)/(1+a) = 0.5 * 1.10/1.20
        assertEquals(0.5 * 1.10 / 1.20, RatioGenerator.baseRatio(50.0, 100.0, 0.20, 0.10), 1e-9)
    }

    @Test
    fun gapRatio_formula() {
        // (P/Q) * (a-b)/(1+a) = 0.5 * (0.20-0.10)/1.20
        assertEquals(0.5 * 0.10 / 1.20, RatioGenerator.gapRatio(50.0, 100.0, 0.20, 0.10), 1e-9)
    }

    @Test
    fun generate_isStructurallyValid() {
        val rnd = Random(4)
        repeat(300) { i ->
            val d = Difficulty.entries[i % 3]
            val q = RatioGenerator.generate(d, rnd)
            assertEquals(QuestionType.RATIO, q.type)
            assertTrue(q.subType in setOf("现期比重", "基期比重", "两期比重差"))
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctAnswer in q.options)
            assertTrue(q.correctAnswer > 0.0)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.RatioGeneratorTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`RatioGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object RatioGenerator {

    fun presentRatio(p: Double, q: Double): Double = p / q
    fun baseRatio(p: Double, q: Double, a: Double, b: Double): Double = (p / q) * (1 + b) / (1 + a)
    fun gapRatio(p: Double, q: Double, a: Double, b: Double): Double = (p / q) * (a - b) / (1 + a)

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val q = GenerationUtil.sampleBase(topic, difficulty, random) // 整体现期
        val a = GenerationUtil.sampleRate(topic, difficulty, random) // 部分增速
        val b = GenerationUtil.sampleRate(topic, difficulty, random) // 整体增速
        val fraction = listOf(0.2, 0.25, 0.3, 0.4, 0.5, 0.6, 0.7).random(random)
        val p = GenerationUtil.roundForDifficulty(q * fraction, difficulty) // 部分现期
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        val (subType, answer, explanation) = when (random.nextInt(3)) {
            0 -> Triple("现期比重", presentRatio(p, q),
                "现期比重 = 部分现期 ÷ 整体现期 = ${NumberUtil.format(p, dec)} ÷ ${NumberUtil.format(q, dec)} = ${NumberUtil.formatPercent(presentRatio(p, q))}")
            1 -> Triple("基期比重", baseRatio(p, q, a, b),
                "基期比重 = (P/Q) × (1+b) ÷ (1+a) = ${NumberUtil.formatPercent(presentRatio(p, q))} × ${NumberUtil.format(1 + b, dec)} ÷ ${NumberUtil.format(1 + a, dec)} = ${NumberUtil.formatPercent(baseRatio(p, q, a, b))}")
            else -> Triple("两期比重差", gapRatio(p, q, a, b),
                "两期比重差 = (P/Q) × (a−b) ÷ (1+a) = ${NumberUtil.formatPercent(presentRatio(p, q))} × ${NumberUtil.formatPercent(a - b)} ÷ ${NumberUtil.format(1 + a, dec)} = ${NumberUtil.formatPercent(gapRatio(p, q, a, b))}")
        }
        val answerRounded = GenerationUtil.roundForDifficulty(answer, difficulty)
        val unit = if (subType == "两期比重差") "个百分点" else "%"
        val stem = "${year}年${topic.name}中，某部分为${NumberUtil.format(p, dec)}${topic.unit}，整体为${NumberUtil.format(q, dec)}${topic.unit}，部分增速${NumberUtil.formatPercent(a)}，整体增速${NumberUtil.formatPercent(b)}，求${subType}。"
        return Question(QuestionType.RATIO, subType, topic.name, stem, answerRounded,
            OptionGenerator.build(answerRounded, difficulty, random), unit, explanation, difficulty)
    }
}
```

> 注：`List.random(random)` 在 Kotlin 2.4 可用。若 IDE 提示弃用，改用 `list[random.nextInt(list.size)]`。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.RatioGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/RatioGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/RatioGeneratorTest.kt
git commit -m "feat(domain): add RatioGenerator"
```

---

### Task 9: AverageGenerator 平均数

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/AverageGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/AverageGeneratorTest.kt`

**Interfaces:**
- Consumes: 基础设施。
- Produces: `object AverageGenerator`，纯函数 `presentAverage(total, count)`、`baseAverage(total, count, a, b)`（均为「元/人」，含 ×10000 量纲换算）；`generate(...)`，`subType` ∈ {"现期平均数", "基期平均数"}，`unit == "元"`。

- [ ] **Step 1: 写失败测试**

`AverageGeneratorTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AverageGeneratorTest {
    @Test
    fun presentAverage_convertsYiWanToYuanPerPerson() {
        // 总量 5000 亿元 ÷ 人数 2500 万人 = 2 亿元/万人 = 20000 元/人
        assertEquals(20000.0, AverageGenerator.presentAverage(5000.0, 2500.0), 1e-6)
    }

    @Test
    fun baseAverage_formula() {
        val expect = (5000.0 / 1.20) / (2500.0 / 1.10) * 10000.0
        assertEquals(expect, AverageGenerator.baseAverage(5000.0, 2500.0, 0.20, 0.10), 1e-6)
    }

    @Test
    fun generate_isStructurallyValid() {
        val rnd = Random(5)
        repeat(300) { i ->
            val d = Difficulty.entries[i % 3]
            val q = AverageGenerator.generate(d, rnd)
            assertEquals(QuestionType.AVERAGE, q.type)
            assertTrue(q.subType in setOf("现期平均数", "基期平均数"))
            assertEquals("元", q.unit)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctAnswer in q.options)
            assertTrue(q.correctAnswer > 0.0)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.AverageGeneratorTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`AverageGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object AverageGenerator {

    private const val PER_CAPITA = 10000.0 // 亿元 ÷ 万人 → 元/人

    fun presentAverage(total: Double, count: Double): Double = total / count * PER_CAPITA
    fun baseAverage(total: Double, count: Double, a: Double, b: Double): Double =
        (total / (1 + a)) / (count / (1 + b)) * PER_CAPITA

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val total = GenerationUtil.sampleBase(topic, difficulty, random)
        val count = GenerationUtil.roundForDifficulty(random.nextDouble(500.0, 5000.0), difficulty)
        val a = GenerationUtil.sampleRate(topic, difficulty, random)
        val b = GenerationUtil.sampleRate(topic, difficulty, random)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        return if (random.nextBoolean()) {
            val answer = presentAverage(total, count)
            val stem = "${year}年${topic.name}总量为${NumberUtil.format(total, dec)}亿元，涉及人数${NumberUtil.format(count, dec)}万人，求现期平均数。"
            val explanation = "现期平均数 = 总量 ÷ 个数 = ${NumberUtil.format(total, dec)} ÷ ${NumberUtil.format(count, dec)} × 10000 = ${NumberUtil.format(answer, dec)}元/人"
            Question(QuestionType.AVERAGE, "现期平均数", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "元", explanation, difficulty)
        } else {
            val answer = baseAverage(total, count, a, b)
            val stem = "${year}年${topic.name}总量为${NumberUtil.format(total, dec)}亿元（增速${NumberUtil.formatPercent(a)}），涉及人数${NumberUtil.format(count, dec)}万人（增速${NumberUtil.formatPercent(b)}），求基期平均数。"
            val explanation = "基期平均数 = (总量÷(1+a)) ÷ (个数÷(1+b)) = (${NumberUtil.format(total, dec)}÷${NumberUtil.format(1 + a, dec)}) ÷ (${NumberUtil.format(count, dec)}÷${NumberUtil.format(1 + b, dec)}) × 10000 = ${NumberUtil.format(answer, dec)}元/人"
            Question(QuestionType.AVERAGE, "基期平均数", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "元", explanation, difficulty)
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.AverageGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/AverageGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/AverageGeneratorTest.kt
git commit -m "feat(domain): add AverageGenerator"
```

---

### Task 10: MultipleGenerator 倍数

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/MultipleGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/MultipleGeneratorTest.kt`

**Interfaces:**
- Consumes: 基础设施。
- Produces: `object MultipleGenerator`，纯函数 `presentMultiple(a, b)`、`baseMultiple(a, b, aRate, bRate)`；`generate(...)`，`subType` ∈ {"现期倍数", "基期倍数"}，`unit == "倍"`。

- [ ] **Step 1: 写失败测试**

`MultipleGeneratorTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultipleGeneratorTest {
    @Test
    fun presentMultiple_formula() {
        assertEquals(2.5, MultipleGenerator.presentMultiple(250.0, 100.0), 1e-9)
    }

    @Test
    fun baseMultiple_formula() {
        val expect = (250.0 / 1.20) / (100.0 / 1.10)
        assertEquals(expect, MultipleGenerator.baseMultiple(250.0, 100.0, 0.20, 0.10), 1e-9)
    }

    @Test
    fun generate_isStructurallyValid() {
        val rnd = Random(6)
        repeat(300) { i ->
            val d = Difficulty.entries[i % 3]
            val q = MultipleGenerator.generate(d, rnd)
            assertEquals(QuestionType.MULTIPLE, q.type)
            assertTrue(q.subType in setOf("现期倍数", "基期倍数"))
            assertEquals("倍", q.unit)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctAnswer in q.options)
            assertTrue(q.correctAnswer >= 1.0)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.MultipleGeneratorTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`MultipleGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object MultipleGenerator {

    fun presentMultiple(a: Double, b: Double): Double = a / b
    fun baseMultiple(a: Double, b: Double, aRate: Double, bRate: Double): Double =
        (a / (1 + aRate)) / (b / (1 + bRate))

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val b = GenerationUtil.sampleBase(topic, difficulty, random) // 较小者
        val m = listOf(1.5, 2.0, 2.5, 3.0, 4.0).random(random)
        val a = GenerationUtil.roundForDifficulty(b * m, difficulty) // 较大者
        val aRate = GenerationUtil.sampleRate(topic, difficulty, random)
        val bRate = GenerationUtil.sampleRate(topic, difficulty, random)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        return if (random.nextBoolean()) {
            val answer = presentMultiple(a, b)
            val stem = "${year}年A地区${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，B地区为${NumberUtil.format(b, dec)}${topic.unit}，求A是B的多少倍。"
            val explanation = "现期倍数 = A ÷ B = ${NumberUtil.format(a, dec)} ÷ ${NumberUtil.format(b, dec)} = ${NumberUtil.format(answer, dec)}倍"
            Question(QuestionType.MULTIPLE, "现期倍数", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "倍", explanation, difficulty)
        } else {
            val answer = baseMultiple(a, b, aRate, bRate)
            val stem = "${year}年A地区${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}（增速${NumberUtil.formatPercent(aRate)}），B地区为${NumberUtil.format(b, dec)}${topic.unit}（增速${NumberUtil.formatPercent(bRate)}），求基期倍数。"
            val explanation = "基期倍数 = (A÷(1+a)) ÷ (B÷(1+b)) = (${NumberUtil.format(a, dec)}÷${NumberUtil.format(1 + aRate, dec)}) ÷ (${NumberUtil.format(b, dec)}÷${NumberUtil.format(1 + bRate, dec)}) = ${NumberUtil.format(answer, dec)}倍"
            Question(QuestionType.MULTIPLE, "基期倍数", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "倍", explanation, difficulty)
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.MultipleGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/MultipleGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/MultipleGeneratorTest.kt
git commit -m "feat(domain): add MultipleGenerator"
```

---

### Task 11: SpecialRateGenerator 特殊增长率

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/SpecialRateGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/SpecialRateGeneratorTest.kt`

**Interfaces:**
- Consumes: 基础设施。
- Produces: `object SpecialRateGenerator`，纯函数 `intervalRate(r1, r2)`、`mixedRate(b1, r1, b2, r2)`；`generate(...)`，`subType` ∈ {"间隔增长率", "混合增长率"}，`unit == "%"`。

- [ ] **Step 1: 写失败测试**

`SpecialRateGeneratorTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialRateGeneratorTest {
    @Test
    fun intervalRate_formula() {
        assertEquals(0.10 + 0.20 + 0.10 * 0.20, SpecialRateGenerator.intervalRate(0.10, 0.20), 1e-9)
    }

    @Test
    fun mixedRate_weightedAverage() {
        val expect = (1000.0 * 0.10 + 2000.0 * 0.20) / (1000.0 + 2000.0)
        assertEquals(expect, SpecialRateGenerator.mixedRate(1000.0, 0.10, 2000.0, 0.20), 1e-9)
    }

    @Test
    fun generate_isStructurallyValid() {
        val rnd = Random(7)
        repeat(300) { i ->
            val d = Difficulty.entries[i % 3]
            val q = SpecialRateGenerator.generate(d, rnd)
            assertEquals(QuestionType.SPECIAL_RATE, q.type)
            assertTrue(q.subType in setOf("间隔增长率", "混合增长率"))
            assertEquals("%", q.unit)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctAnswer in q.options)
            assertTrue(q.correctAnswer > 0.0)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.SpecialRateGeneratorTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`SpecialRateGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object SpecialRateGenerator {

    fun intervalRate(r1: Double, r2: Double): Double = r1 + r2 + r1 * r2
    fun mixedRate(b1: Double, r1: Double, b2: Double, r2: Double): Double =
        (b1 * r1 + b2 * r2) / (b1 + b2)

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        return if (random.nextBoolean()) {
            // 间隔增长率：r = r1 + r2 + r1·r2
            val r1 = GenerationUtil.sampleRate(topic, difficulty, random).coerceAtLeast(0.0)
            val r2 = GenerationUtil.sampleRate(topic, difficulty, random).coerceAtLeast(0.0)
            val answer = intervalRate(r1, r2)
            val stem = "${year - 2}年${topic.name}增长${NumberUtil.formatPercent(r1)}，${year - 1}年增长${NumberUtil.formatPercent(r2)}，求${year - 2}年—${year}年${topic.name}的间隔增长率。"
            val explanation = "间隔增长率 = r₁ + r₂ + r₁×r₂ = ${NumberUtil.formatPercent(r1)} + ${NumberUtil.formatPercent(r2)} + ${NumberUtil.formatPercent(r1)}×${NumberUtil.formatPercent(r2)} = ${NumberUtil.formatPercent(answer)}"
            Question(QuestionType.SPECIAL_RATE, "间隔增长率", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "%", explanation, difficulty)
        } else {
            // 混合增长率：r = (B₁r₁ + B₂r₂) ÷ (B₁ + B₂)
            val b1 = GenerationUtil.sampleBase(topic, difficulty, random)
            val b2 = GenerationUtil.sampleBase(topic, difficulty, random)
            val r1 = GenerationUtil.sampleRate(topic, difficulty, random)
            val r2 = GenerationUtil.sampleRate(topic, difficulty, random)
            val answer = mixedRate(b1, r1, b2, r2)
            val stem = "${year}年${topic.name}中，A部分为${NumberUtil.format(b1, dec)}${topic.unit}（增速${NumberUtil.formatPercent(r1)}），B部分为${NumberUtil.format(b2, dec)}${topic.unit}（增速${NumberUtil.formatPercent(r2)}），求整体混合增长率。"
            val explanation = "混合增长率 = (B₁r₁ + B₂r₂) ÷ (B₁ + B₂) = (${NumberUtil.format(b1, dec)}×${NumberUtil.formatPercent(r1)} + ${NumberUtil.format(b2, dec)}×${NumberUtil.formatPercent(r2)}) ÷ ${NumberUtil.format(b1 + b2, dec)} = ${NumberUtil.formatPercent(answer)}"
            Question(QuestionType.SPECIAL_RATE, "混合增长率", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "%", explanation, difficulty)
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.SpecialRateGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/SpecialRateGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/SpecialRateGeneratorTest.kt
git commit -m "feat(domain): add SpecialRateGenerator"
```

---

### Task 12: QuestionGenerator 分发器

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/generator/QuestionGenerator.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/generator/QuestionGeneratorTest.kt`

**Interfaces:**
- Consumes: 7 个生成器 + `QuestionType`。
- Produces: `class QuestionGenerator(seed: Long? = null)` with `fun generate(type, difficulty): Question`、`fun generateRandom(types: List<QuestionType>, difficulty): Question`。

- [ ] **Step 1: 写失败测试**

`QuestionGeneratorTest.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionGeneratorTest {
    @Test
    fun generate_dispatchesToCorrectType() {
        val gen = QuestionGenerator(seed = 2024)
        for (type in QuestionType.entries) {
            val q = gen.generate(type, Difficulty.EASY)
            assertEquals(type, q.type)
            assertEquals(Difficulty.EASY, q.difficulty)
        }
    }

    @Test
    fun generate_sameSeedProducesSameQuestion() {
        val a = QuestionGenerator(seed = 99).generate(QuestionType.RATIO, Difficulty.MEDIUM)
        val b = QuestionGenerator(seed = 99).generate(QuestionType.RATIO, Difficulty.MEDIUM)
        assertEquals(a, b)
    }

    @Test
    fun generateRandom_onlyFromGivenTypes() {
        val gen = QuestionGenerator(seed = 7)
        val types = listOf(QuestionType.BASE_PERIOD, QuestionType.MULTIPLE)
        repeat(100) {
            assertTrue(gen.generateRandom(types, Difficulty.EASY).type in types)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.QuestionGeneratorTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`QuestionGenerator.kt`:

```kotlin
package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random

class QuestionGenerator(seed: Long? = null) {
    private val random: Random = seed?.let { Random(it) } ?: Random.Default

    fun generate(type: QuestionType, difficulty: Difficulty): Question = when (type) {
        QuestionType.BASE_PERIOD -> BasePeriodGenerator.generate(difficulty, random)
        QuestionType.GROWTH_AMOUNT -> GrowthAmountGenerator.generate(difficulty, random)
        QuestionType.GROWTH_RATE -> GrowthRateGenerator.generate(difficulty, random)
        QuestionType.RATIO -> RatioGenerator.generate(difficulty, random)
        QuestionType.AVERAGE -> AverageGenerator.generate(difficulty, random)
        QuestionType.MULTIPLE -> MultipleGenerator.generate(difficulty, random)
        QuestionType.SPECIAL_RATE -> SpecialRateGenerator.generate(difficulty, random)
    }

    fun generateRandom(types: List<QuestionType>, difficulty: Difficulty): Question {
        require(types.isNotEmpty()) { "types 不能为空" }
        return generate(types[random.nextInt(types.size)], difficulty)
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.generator.QuestionGeneratorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/generator/QuestionGenerator.kt app/src/test/java/com/example/quickcalculation/domain/generator/QuestionGeneratorTest.kt
git commit -m "feat(domain): add QuestionGenerator dispatcher"
```

---

### Task 13: AnswerJudge 判分器

**Files:**
- Create: `app/src/main/java/com/example/quickcalculation/domain/judge/AnswerJudge.kt`
- Test: `app/src/test/java/com/example/quickcalculation/domain/judge/AnswerJudgeTest.kt`

**Interfaces:**
- Consumes: `Question`、`Difficulty`、`NumberUtil`。
- Produces: `object AnswerJudge { fun judgeChoice(question, selectedIndex): Boolean; fun judgeFill(question, userAnswer, tolerance): Boolean; fun defaultTolerance(difficulty): Double }`。

- [ ] **Step 1: 写失败测试**

`AnswerJudgeTest.kt`:

```kotlin
package com.example.quickcalculation.domain.judge

import com.example.quickcalculation.domain.generator.QuestionGenerator
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerJudgeTest {
    private val q = QuestionGenerator(seed = 1).generate(QuestionType.BASE_PERIOD, Difficulty.EASY)

    @Test
    fun judgeChoice_correctIndex() {
        val correctIdx = q.options.indexOf(q.correctAnswer)
        assertTrue(AnswerJudge.judgeChoice(q, correctIdx))
    }

    @Test
    fun judgeChoice_wrongIndex() {
        val wrongIdx = q.options.indices.first { q.options[it] != q.correctAnswer }
        assertFalse(AnswerJudge.judgeChoice(q, wrongIdx))
    }

    @Test
    fun judgeChoice_outOfRange() {
        assertFalse(AnswerJudge.judgeChoice(q, -1))
        assertFalse(AnswerJudge.judgeChoice(q, q.options.size))
    }

    @Test
    fun judgeFill_withinTolerance() {
        assertTrue(AnswerJudge.judgeFill(q, q.correctAnswer * 1.001, 0.005))
        assertFalse(AnswerJudge.judgeFill(q, q.correctAnswer * 1.02, 0.005))
    }

    @Test
    fun defaultTolerance_hardIsTighter() {
        assertEquals(0.005, AnswerJudge.defaultTolerance(Difficulty.EASY), 0.0)
        assertEquals(0.002, AnswerJudge.defaultTolerance(Difficulty.HARD), 0.0)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew testDebugUnitTest --tests "com.example.quickcalculation.domain.judge.AnswerJudgeTest"`
Expected: 编译失败。

- [ ] **Step 3: 写实现**

`AnswerJudge.kt`:

```kotlin
package com.example.quickcalculation.domain.judge

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.util.NumberUtil

object AnswerJudge {

    fun judgeChoice(question: Question, selectedIndex: Int): Boolean {
        if (selectedIndex !in question.options.indices) return false
        return question.options[selectedIndex] == question.correctAnswer
    }

    fun judgeFill(question: Question, userAnswer: Double, tolerance: Double): Boolean =
        NumberUtil.withinTolerance(userAnswer, question.correctAnswer, tolerance)

    fun defaultTolerance(difficulty: Difficulty): Double = when (difficulty) {
        Difficulty.EASY -> 0.005
        Difficulty.MEDIUM -> 0.005
        Difficulty.HARD -> 0.002
    }
}
```

- [ ] **Step 4: 运行测试确认通过（全量回归）**

Run: `./gradlew testDebugUnitTest`
Expected: 全部测试 PASS（含 Task 1–12 的所有测试）。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/quickcalculation/domain/judge app/src/test/java/com/example/quickcalculation/domain/judge
git commit -m "feat(domain): add AnswerJudge"
```

---

## 完成标准（M0 + M1 Done Definition）

- [ ] `./gradlew testDebugUnitTest` 全绿，无编译错误。
- [ ] `domain/` 下无任何 `android.*` import。
- [ ] 7 大类题型 + 判分器 + 题材库 + 工具类全部实现并测试。
- [ ] 每个生成器满足：4 选项唯一正确、增长率落在题材区间、`Random(seed)` 可复现、数学恒等式成立。

**后续（不在本计划内）**：M2 `QuizViewModel`（StateFlow 状态管理）→ M3 Compose UI（题型选择 + 刷题 + 主题切换）→ M4 打包验证。
