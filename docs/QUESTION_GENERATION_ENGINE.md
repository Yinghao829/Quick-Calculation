# 行测·资料分析刷题 App —— 题目生成引擎方案

> 项目：Quick Calculation（速算练习）  
> 技术栈：Kotlin + Jetpack Compose  
> 目标：自动生成符合现实数据特征、计算合理、难度可控的资料分析练习题。

---

## 1. 核心目标

题目不能简单依靠 `Random` 随机生成，而应采用**约束随机生成**：

```text
现实场景
  ↓
数据模板
  ↓
生成关键数据
  ↓
反推其他数据
  ↓
约束检查
  ↓
生成题目
  ↓
生成选项与解析
```

核心原则：

1. 现实合理优先于随机性。
2. 先确定数据结构，再随机参数。
3. 尽量保证数据之间存在真实统计关系。
4. 难度由计算复杂度决定，而不是单纯放大数字。
5. 所有数据生成后必须经过校验。

---

## 2. 三个难度

### EASY：基础

特点：

- 整数为主
- 常见增长率：5%、10%、20%、25%
- 数据关系整齐
- 单步计算
- 主要用于熟悉公式

示例：

```text
2025年粮食产量240万吨，同比增长20%，
求2024年产量。

240 ÷ 1.2 = 200万吨
```

### NORMAL：标准

特点：

- 允许小数
- 增长率增加8%、12.5%、15%等
- 开始出现估算
- 两步计算
- 接近普通行测训练题

### HARD：进阶

特点：

- 允许负增长
- 复杂增长率
- 多条件计算
- 间隔增长率
- 比重变化
- 混合增长率
- 多步骤计算和估算

---

## 3. 现实场景库

建立 `Scenario`：

```kotlin
data class Scenario(
    val category: String,
    val subject: String,
    val unit: String,
    val minValue: Double,
    val maxValue: Double,
    val growthMin: Double,
    val growthMax: Double
)
```

第一版建议包含：

### 农业

- 粮食产量
- 蔬菜产量
- 肉类产量
- 水产品产量
- 农业增加值

### 工业

- 工业增加值
- 汽车产量
- 新能源汽车产量
- 发电量
- 钢材产量

### 交通

- 旅客运输量
- 货物运输量
- 快递业务量
- 港口货物吞吐量

### 消费

- 社会消费品零售总额
- 网上零售额
- 餐饮收入
- 商品销售额

### 人口与教育

- 常住人口
- 就业人数
- 在校学生人数
- 高校毕业生

### 旅游

- 旅游人数
- 旅游收入
- 入境游客人数

> 数据范围用于限制数量级，不代表在整个区间内均匀随机。

---

## 4. 数据生成原则

### 4.1 不直接随机四个基本量

不要：

```text
随机 A
随机 B
随机 X
随机 r
```

而应：

```text
确定 B
 ↓
确定 r
 ↓
计算 A
 ↓
计算 X
```

例如：

```text
B = 500
r = 20%

A = 500 × 1.2 = 600
X = 600 - 500 = 100
```

这样可以保证：

```text
A = B × (1+r)
X = A-B
r = X/B
```

始终成立。

---

## 5. 增长率生成

不要直接使用：

```kotlin
Random.nextDouble(-0.2, 0.3)
```

而使用增长率池。

### EASY

```text
5%、10%、20%、25%
```

### NORMAL

```text
5%、8%、10%、12.5%、15%、20%、25%
```

### HARD

```text
-20%、-15%、-12.5%、-10%、-8%、-5%
7.5%、12.5%、15%、17.5%、18%、20%、22.5%、25%
```

不同增长率采用不同概率，常见数值出现概率更高。

> **当前实现批注（截至 2026-09-09）**
>
> 本节以上内容是原始设计方案，暂时保留不删除。当前代码已经按照“固定增长率池 + 权重随机”的方式实现，但还没有实现连续随机增长率。
>
> 当前实现位置：
>
> - `app/src/main/java/com/example/quickcalculation/domain/generator/GrowthRatePool.kt`
> - `app/src/main/java/com/example/quickcalculation/domain/generator/GenerationUtil.kt`
>
> 当前增长率数据不是任意连续小数，而是预先配置的离散候选值：
>
> - EASY：`5%`、`8%`、`10%`、`15%`、`20%`、`25%`，以及 `-5%`、`-10%` 等。
> - MEDIUM：在常见增长率基础上增加 `2.3%`、`3.4%`、`5.1%`、`9.1%`、`11.4%`、`13.6%`、`16.2%` 等不规则值，并允许部分负增长。
> - HARD：包含 `0.9%`、`2.3%`、`3.4%`、`5.1%`、`5.3%`、`6.7%`、`9.1%`、`9.6%`、`11.2%`、`14.2%`、`16.8%`、`21.3%`、`23.8%` 等值，同时包含 `-2%` 至 `-20%` 的负增长率。
>
> 每个候选值使用 `rate to weight` 表示增长率和权重。例如：
>
> ```kotlin
> 0.125 to 2
> ```
>
> 表示 `12.5%` 这个增长率，权重为 `2`。权重越大，被抽中的概率越高，但它仍然只能从固定列表中抽取。
>
> 当前 `sampleRate(...)` 的实际流程如下：
>
> ```text
> 按难度选择固定增长率池
>     ↓
> 按权重抽取一个候选值
>     ↓
> 检查是否位于当前题材 growthRange
>     ↓
> 最多重试 40 次
>     ↓
> 若仍未命中，使用 0.5% 步长在题材范围内随机兜底
> ```
>
> 当前题材范围定义在 `TopicRepository.kt`，例如：
>
> - GDP：`2%..12%`
> - 社会消费品零售总额：`3%..15%`
> - 进出口总额：`-15%..20%`
> - 固定资产投资：`-10%..15%`
> - 常住人口：`0%..2%`
> - 居民消费价格：`0%..4%`
>
> 因此，当前实现已经具备“题材范围约束”，但不具备“范围内任意增长率连续随机”和“小幅值高概率、大幅值低概率”的完整分布模型。当前的概率只作用于候选列表权重，不是连续区间上的概率密度。
>
> 另外，基础数据的当前实现位于 `GenerationUtil.sampleBase(...)`：先在题材的 `magnitude` 范围内均匀随机，再根据难度舍入；简单难度额外调整为整十。因此，当前基础数据也还没有实现“小数据概率高、大数据概率低”的偏置分布。
>
> 后续若采用连续随机方案，应保留 `growthRange` 作为题材硬边界，在边界内使用分段概率和偏向小值的连续采样，并在生成 `A`、`B`、`X` 后继续执行数据关系校验。详细改造方案见 `docs/CONTINUOUS_GROWTH_RATE_DESIGN.md`。

---

## 6. 数据模板

```kotlin
data class DataTemplate(
    val name: String,
    val difficulty: Difficulty
)
```

### EASY

- 整百数 + 10%
- 整百数 + 20%
- 整数 + 25%
- 增长量为整十数

### NORMAL

- 整数 + 12.5%
- 整数 + 15%
- 已知现期、基期计算增长率
- 部分 + 整体 + 增长率

### HARD

- 两年增长率
- 部分增长率 + 整体增长率
- 负增长
- 多条件组合
- 需要估算的数据

---

## 7. 数据约束检查

建立：

```kotlin
object ConstraintValidator {

    fun validate(data: GeneratedData): Boolean {
        return validateRange(data)
            && validatePositive(data)
            && validateGrowthRelation(data)
            && validateCalculationDifficulty(data)
    }
}
```

主要检查：

### 基本关系

```text
A > 0
B > 0
```

### 增长方向

```text
r > 0 → A > B
r < 0 → A < B
r = 0 → A = B
```

### 增长量

```text
X = A - B
```

### 比重

```text
0 < P < Q
0 < P/Q < 1
```

### 倍数

```text
A >= B
A/B >= 1
```

### 现实性

检查：

- 数量级
- 单位
- 增长率
- 年份关系
- 正负关系

不合理则重新生成。

---

## 8. 7 大题型

### 8.1 基期与现期

```text
B = A ÷ (1+r)
A = B × (1+r)
```

### 8.2 增长量

```text
X = A-B
X = A×r/(1+r)
```

### 8.3 一般增长率

```text
r = (A-B)/B
r = X/B
```

### 8.4 比重

```text
现期比重 = P/Q

基期比重 = (P/Q)×(1+b)/(1+a)

两期比重差 = (P/Q)×(a-b)/(1+a)
```

### 8.5 平均数

```text
现期平均数 = 总量/个数

基期平均数 =
(总量/(1+a))/(个数/(1+b))
```

### 8.6 倍数

```text
现期倍数 = A/B

基期倍数 =
(A/(1+a))/(B/(1+b))
```

### 8.7 特殊增长率

间隔增长率：

```text
r = r1+r2+r1×r2
```

混合增长率：

```text
整体增速位于两个部分增速之间，
并偏向基期量较大的一方。
```

---

## 9. DataContext

建议不要让每道题完全独立随机生成。

```kotlin
data class DataContext(
    val scenario: Scenario,
    val years: List<Int>,
    val indicators: List<IndicatorData>
)
```

例如：

```text
某市经济发展情况

                2024    2025
GDP             5000    5500
工业增加值      2000    2200
消费总额        1500    1650
进出口总额       800     920
```

然后从同一份资料生成：

- 增长量题
- 增长率题
- 比重题
- 倍数题
- 平均数题

这样更接近真实资料分析。

---

## 10. 题目模型

```kotlin
data class Question(
    val type: QuestionType,
    val subType: String,
    val difficulty: Difficulty,
    val scenario: String,
    val stem: String,
    val correctAnswer: Double,
    val options: List<Double>,
    val unit: String,
    val explanation: String,
    val dataContext: DataContext?
)
```

---

## 11. 干扰项生成

4个选项：

```text
1个正确答案
3个干扰答案
```

干扰项来源：

1. 公式错误
2. 正负号错误
3. 基期/现期概念混淆
4. 增长量/增长率混淆
5. 四舍五入错误

必须保证：

```text
4个选项不重复
只有一个正确答案
干扰项数量级接近
```

---

## 12. 解析生成

答案和解析必须来自同一次计算。

流程：

```text
生成数据
 ↓
计算答案
 ↓
保存计算过程
 ↓
生成解析
```

例如：

```text
基期量 = 现期量 ÷ (1+增长率)
       = 240 ÷ 1.2
       = 200万吨
```

---

## 13. 随机种子

必须支持：

```kotlin
Random(seed)
```

作用：

- 复现题目
- 单元测试
- Bug定位
- 调试

---

## 14. 生成流程

```text
generateQuestion()
        ↓
选择题型
        ↓
选择难度
        ↓
选择现实场景
        ↓
选择数据模板
        ↓
生成关键参数
        ↓
反推其他数据
        ↓
计算派生数据
        ↓
ConstraintValidator
        ↓
   ┌────┴────┐
不合格       合格
   │           ↓
重新生成    生成题目
               ↓
          生成干扰项
               ↓
           生成解析
               ↓
             Question
```

---

## 15. 推荐项目结构

```text
domain/
├── model/
│   ├── Question.kt
│   ├── QuestionType.kt
│   ├── Difficulty.kt
│   ├── Scenario.kt
│   ├── DataContext.kt
│   ├── IndicatorData.kt
│   └── GeneratedData.kt
│
├── generator/
│   ├── QuestionGenerator.kt
│   ├── data/
│   │   ├── DataGenerator.kt
│   │   ├── ScenarioGenerator.kt
│   │   ├── GrowthRateGenerator.kt
│   │   ├── DataTemplate.kt
│   │   └── DataTemplateRegistry.kt
│   ├── constraint/
│   │   ├── ConstraintValidator.kt
│   │   ├── RealismValidator.kt
│   │   └── DifficultyValidator.kt
│   ├── BasePeriodGenerator.kt
│   ├── GrowthAmountGenerator.kt
│   ├── GrowthRateGenerator.kt
│   ├── RatioGenerator.kt
│   ├── AverageGenerator.kt
│   ├── MultipleGenerator.kt
│   └── SpecialRateGenerator.kt
│
├── option/
│   └── OptionGenerator.kt
│
├── explanation/
│   └── ExplanationBuilder.kt
│
└── template/
    └── QuestionTemplate.kt
```

---

## 16. 测试要求

至少测试：

### 数学正确性

验证：

```text
A = B×(1+r)
X = A-B
r = X/B
```

### 数据合理性

- 数值范围
- 单位
- 增长方向
- 比重范围
- 倍数关系

### 选项

- 4个选项不重复
- 唯一正确答案

### 难度

分别生成大量题目，检查：

- 计算步骤
- 小数比例
- 增长率复杂程度
- 估算比例

---

## 17. V1 实现范围

第一版不需要过度复杂：

```text
现实场景：20~30种
数据模板：10~20种
增长率：固定概率池
难度：3级
题型：7大类
```

第一阶段重点保证：

```text
数学正确
+
现实合理
+
计算可行
+
难度匹配
+
选项合理
+
解析正确
```

---

## 18. 后续扩展

### V2

增加：

- 更多场景
- 更多数据模板
- 更复杂干扰项
- 更多题型
- 错题统计

### V3

升级为完整资料生成：

```text
一份统计材料
    ↓
自动生成5道题
    ↓
增长量
增长率
比重
倍数
综合判断
```

最终形成：

> **现实数据模型 + 约束随机 + 难度控制 + 自动出题 + 自动解析**

的资料分析题目生成引擎。
