package com.example.quickcalculation.viewmodel

import com.example.quickcalculation.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class UiEnumTest {
    @Test
    fun themeMode_hasThreeValues() {
        assertEquals(3, ThemeMode.entries.size)
    }

    @Test
    fun answerMode_hasTwoValues() {
        assertEquals(2, AnswerMode.entries.size)
        assertEquals(AnswerMode.CHOICE, AnswerMode.valueOf("CHOICE"))
        assertEquals(AnswerMode.FILL, AnswerMode.valueOf("FILL"))
    }
}
