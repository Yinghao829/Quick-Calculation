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
