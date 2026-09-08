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
