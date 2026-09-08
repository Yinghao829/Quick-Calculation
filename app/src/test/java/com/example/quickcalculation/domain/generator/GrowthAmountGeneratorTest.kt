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
