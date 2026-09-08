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
