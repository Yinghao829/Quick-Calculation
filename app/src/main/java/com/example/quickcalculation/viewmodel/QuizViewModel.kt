package com.example.quickcalculation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.quickcalculation.domain.generator.QuestionGenerator
import com.example.quickcalculation.domain.judge.AnswerJudge
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.model.QuizStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QuizViewModel(
    private val generator: QuestionGenerator = QuestionGenerator(),
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    fun toggleType(type: QuestionType) = _state.update { s ->
        val set = if (type in s.selectedTypes) s.selectedTypes - type else s.selectedTypes + type
        s.copy(selectedTypes = set)
    }

    fun setDifficulty(d: Difficulty) = _state.update { it.copy(difficulty = d) }

    fun setAnswerMode(m: AnswerMode) = _state.update { it.copy(answerMode = m) }

    fun startQuiz() = _state.update { s ->
        if (s.selectedTypes.isEmpty()) return@update s
        s.copy(question = generator.generateRandom(s.selectedTypes.toList(), s.difficulty), stats = QuizStats())
    }

    fun nextQuestion() = _state.update { s ->
        val q = s.question ?: return@update s
        s.copy(question = generator.generateRandom(s.selectedTypes.toList(), s.difficulty),
            selectedOption = null, fillInput = "", answered = false, isCorrect = null)
    }

    fun selectOption(index: Int) = _state.update { s ->
        val q = s.question ?: return@update s
        if (s.answered || index !in q.options.indices) return@update s
        val correct = AnswerJudge.judgeChoice(q, index)
        s.copy(selectedOption = index, answered = true, isCorrect = correct,
            stats = s.stats.copy(answered = s.stats.answered + 1, correct = s.stats.correct + if (correct) 1 else 0))
    }

    fun setFillInput(text: String) = _state.update { it.copy(fillInput = text) }

    fun submitFill(text: String) = _state.update { s ->
        val q = s.question ?: return@update s
        if (s.answered) return@update s
        val value = text.toDoubleOrNull() ?: return@update s.copy(fillInput = text)
        val tolerance = AnswerJudge.defaultTolerance(s.difficulty)
        val correct = AnswerJudge.judgeFill(q, value, tolerance)
        s.copy(fillInput = text, answered = true, isCorrect = correct,
            stats = s.stats.copy(answered = s.stats.answered + 1, correct = s.stats.correct + if (correct) 1 else 0))
    }
}
