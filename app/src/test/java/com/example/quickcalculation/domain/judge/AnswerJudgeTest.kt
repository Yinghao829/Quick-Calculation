package com.example.quickcalculation.domain.judge

import com.example.quickcalculation.domain.generator.QuestionGenerator
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerJudgeTest {
    private val q = QuestionGenerator(seed = 1).generate(QuestionType.BASE_PERIOD, Difficulty.EASY)

    @Test
    fun judgeChoice_correctIndex() {
        val correctIdx = q.options.indexOf(q.correctAnswer)
        assertTrue(AnswerJudge.judgeChoice(q, correctIdx))
    }

    @Test
    fun judgeChoice_wrongIndex() {
        val wrongIdx = q.options.indices.first { q.options[it] != q.correctAnswer }
        assertFalse(AnswerJudge.judgeChoice(q, wrongIdx))
    }

    @Test
    fun judgeChoice_outOfRange() {
        assertFalse(AnswerJudge.judgeChoice(q, -1))
        assertFalse(AnswerJudge.judgeChoice(q, q.options.size))
    }

    @Test
    fun judgeFill_withinTolerance() {
        assertTrue(AnswerJudge.judgeFill(q, q.correctAnswer * 1.001, 0.005))
        assertFalse(AnswerJudge.judgeFill(q, q.correctAnswer * 1.02, 0.005))
    }

    @Test
    fun defaultTolerance_hardIsTighter() {
        assertEquals(0.005, AnswerJudge.defaultTolerance(Difficulty.EASY), 0.0)
        assertEquals(0.002, AnswerJudge.defaultTolerance(Difficulty.HARD), 0.0)
    }
}
