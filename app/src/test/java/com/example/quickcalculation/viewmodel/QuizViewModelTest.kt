package com.example.quickcalculation.viewmodel

import com.example.quickcalculation.domain.generator.QuestionGenerator
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.QuestionType
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizViewModelTest {

    private fun vm() = QuizViewModel(QuestionGenerator(seed = 42))

    @Test
    fun startQuiz_generatesQuestionAndResetsStats() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.startQuiz()
        val s = vm.state.value
        assertNotNull(s.question)
        assertEquals(0, s.stats.answered)
        assertEquals(QuestionType.BASE_PERIOD, s.question!!.type)
    }

    @Test
    fun toggleType_addsAndRemoves() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.toggleType(QuestionType.RATIO)
        assertTrue(vm.state.value.selectedTypes.containsAll(listOf(QuestionType.BASE_PERIOD, QuestionType.RATIO)))
        vm.toggleType(QuestionType.BASE_PERIOD)
        assertFalse(QuestionType.BASE_PERIOD in vm.state.value.selectedTypes)
    }

    @Test
    fun selectOption_correctAnswer_marksCorrectAndIncrementsStats() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.startQuiz()
        val correctIdx = vm.state.value.question!!.options.indexOf(vm.state.value.question!!.correctAnswer)
        vm.selectOption(correctIdx)
        val s = vm.state.value
        assertTrue(s.answered)
        assertEquals(true, s.isCorrect)
        assertEquals(1, s.stats.answered)
        assertEquals(1, s.stats.correct)
    }

    @Test
    fun selectOption_wrongAnswer_marksIncorrect() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.startQuiz()
        val q = vm.state.value.question!!
        val wrongIdx = q.options.indices.first { q.options[it] != q.correctAnswer }
        vm.selectOption(wrongIdx)
        val s = vm.state.value
        assertTrue(s.answered)
        assertEquals(false, s.isCorrect)
        assertEquals(1, s.stats.answered)
        assertEquals(0, s.stats.correct)
    }

    @Test
    fun submitFill_withinTolerance_marksCorrect() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.setAnswerMode(AnswerMode.FILL)
        vm.startQuiz()
        val correct = vm.state.value.question!!.correctAnswer
        vm.submitFill((correct * 1.001).toString())
        val s = vm.state.value
        assertTrue(s.answered)
        assertEquals(true, s.isCorrect)
    }

    @Test
    fun submitFill_invalidInput_doesNotJudge() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.setAnswerMode(AnswerMode.FILL)
        vm.startQuiz()
        vm.submitFill("not a number")
        val s = vm.state.value
        assertFalse(s.answered)
        assertEquals("not a number", s.fillInput)
    }

    @Test
    fun nextQuestion_clearsAnswerState() {
        val vm = vm()
        vm.toggleType(QuestionType.BASE_PERIOD)
        vm.startQuiz()
        val correctIdx = vm.state.value.question!!.options.indexOf(vm.state.value.question!!.correctAnswer)
        vm.selectOption(correctIdx)
        vm.nextQuestion()
        val s = vm.state.value
        assertFalse(s.answered)
        assertNull(s.isCorrect)
        assertEquals(1, s.stats.answered) // stats 保留
    }

    @Test
    fun startQuiz_withoutTypes_doesNothing() {
        val vm = vm()
        vm.startQuiz()
        assertNull(vm.state.value.question)
    }
}
