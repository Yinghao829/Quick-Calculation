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
