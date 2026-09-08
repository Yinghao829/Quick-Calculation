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

    @Test
    fun generate_easyAnswersArePercentagePointsNotZero() {
        val rnd = Random(3)
        var nonZero = 0
        repeat(100) {
            val q = GrowthRateGenerator.generate(Difficulty.EASY, rnd)
            if (q.correctAnswer != 0.0) nonZero++
        }
        // 若答案以小数分数存储（0.05~0.25），EASY 的 decimals=0 会全舍为 0；
        // 以百分点存储则常见增速（5/10/20/25%）答案非零，只有 常住人口 可能为 0。
        assertTrue("EASY 增长率答案应为非零百分点，实际非零 $nonZero/100", nonZero > 50)
    }
}
