package com.example.quickcalculation.domain.model

data class Question(
    val type: QuestionType,
    val subType: String,
    val topic: String,
    val stem: String,
    val correctAnswer: Double,
    val options: List<Double>,
    val unit: String,
    val explanation: String,
    val difficulty: Difficulty,
)
