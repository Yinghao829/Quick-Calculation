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
