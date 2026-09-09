package com.example.quickcalculation.viewmodel

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.model.QuizStats

data class QuizUiState(
    val selectedTypes: Set<QuestionType> = emptySet(),
    val difficulty: Difficulty = Difficulty.EASY,
    val answerMode: AnswerMode = AnswerMode.CHOICE,
    val question: Question? = null,
    val selectedOption: Int? = null,
    val fillInput: String = "",
    val answered: Boolean = false,
    val isCorrect: Boolean? = null,
    val stats: QuizStats = QuizStats(),
)
