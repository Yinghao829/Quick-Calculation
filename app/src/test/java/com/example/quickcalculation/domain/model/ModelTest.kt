package com.example.quickcalculation.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {
    @Test
    fun questionType_hasSevenTypesWithChineseLabels() {
        assertEquals(7, QuestionType.entries.size)
        assertEquals("基期与现期", QuestionType.BASE_PERIOD.label)
        assertEquals("特殊增长率", QuestionType.SPECIAL_RATE.label)
    }

    @Test
    fun quizStats_accuracy_zeroWhenUnanswered() {
        assertEquals(0f, QuizStats().accuracy)
    }

    @Test
    fun quizStats_accuracy_isCorrectOverAnswered() {
        assertEquals(0.6f, QuizStats(answered = 5, correct = 3).accuracy)
    }

    @Test
    fun topic_hasExpectedFields() {
        val t = Topic("国内生产总值", "亿元", 80000.0..150000.0, 0.02..0.08)
        assertEquals("亿元", t.unit)
        assertTrue(120000.0 in t.magnitude)
        assertTrue(0.05 in t.growthRange)
    }
}
