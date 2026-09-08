package com.example.quickcalculation.domain.model

data class QuizStats(
    val answered: Int = 0,
    val correct: Int = 0,
) {
    val accuracy: Float get() = if (answered == 0) 0f else correct.toFloat() / answered
}
