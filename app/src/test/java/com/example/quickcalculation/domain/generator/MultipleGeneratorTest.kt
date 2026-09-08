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
