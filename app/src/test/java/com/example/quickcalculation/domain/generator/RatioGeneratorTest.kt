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
