package com.example.quickcalculation.domain.judge

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.util.NumberUtil

object AnswerJudge {

    fun judgeChoice(question: Question, selectedIndex: Int): Boolean {
        if (selectedIndex !in question.options.indices) return false
        return question.options[selectedIndex] == question.correctAnswer
    }

    fun judgeFill(question: Question, userAnswer: Double, tolerance: Double): Boolean =
        NumberUtil.withinTolerance(userAnswer, question.correctAnswer, tolerance)

    fun defaultTolerance(difficulty: Difficulty): Double = when (difficulty) {
        Difficulty.EASY -> 0.005
        Difficulty.MEDIUM -> 0.005
        Difficulty.HARD -> 0.002
    }
}
