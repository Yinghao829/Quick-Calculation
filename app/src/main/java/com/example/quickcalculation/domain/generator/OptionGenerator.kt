package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import kotlin.math.abs
import kotlin.random.Random

object OptionGenerator {

    /** 生成 4 个乱序选项：1 个正确项 + 3 个干扰项（难度控制差距，量级接近、互不重复）。 */
    fun build(
        correct: Double,
        difficulty: Difficulty,
        random: Random,
        extra: List<Double> = emptyList(),
    ): List<Double> {
        val offsets = when (difficulty) {
            Difficulty.EASY -> listOf(-0.10, 0.10, -0.05, 0.05, 0.20, -0.20, 0.15, -0.15)
            Difficulty.MEDIUM -> listOf(-0.04, 0.04, -0.02, 0.02, -0.08, 0.08, -0.06, 0.06)
            Difficulty.HARD -> listOf(-0.01, 0.01, -0.005, 0.005, -0.02, 0.02, -0.015, 0.015)
        }
        val candidates = buildList {
            extra.forEach { add(GenerationUtil.roundForDifficulty(it, difficulty)) }
            offsets.forEach { add(GenerationUtil.roundForDifficulty(correct * (1 + it), difficulty)) }
        }
        var distractors = candidates
            .filter { it != correct }
            .filter { it > 0.0 || correct <= 0.0 }
            .distinct()

        // 当 correct 接近 0 时，乘法扰动会坍缩回 0，改用绝对差值生成干扰项。
        val absoluteSteps = when (difficulty) {
            Difficulty.EASY -> listOf(-1.0, 1.0, -2.0, 2.0, -3.0, 3.0, -4.0, 4.0)
            Difficulty.MEDIUM -> listOf(-0.5, 0.5, -1.0, 1.0, -1.5, 1.5, -2.0, 2.0)
            Difficulty.HARD -> listOf(-0.05, 0.05, -0.10, 0.10, -0.15, 0.15, -0.20, 0.20)
        }
        if (distractors.size < 3 && abs(correct) < 1e-9) {
            var i = 0
            while (distractors.size < 3 && i < absoluteSteps.size) {
                val v = GenerationUtil.roundForDifficulty(correct + absoluteSteps[i], difficulty)
                if (v != correct && v !in distractors) distractors = distractors + v
                i++
            }
        }

        var k = 2
        while (distractors.size < 3 && k < 30) {
            val up = GenerationUtil.roundForDifficulty(correct * (1 + 0.05 * k), difficulty)
            val down = GenerationUtil.roundForDifficulty(correct * (1 - 0.05 * k), difficulty)
            if (up != correct && (up > 0.0 || correct <= 0.0) && up !in distractors) distractors = distractors + up
            if (down != correct && (down > 0.0 || correct <= 0.0) && down !in distractors) distractors = distractors + down
            k++
        }

        return (distractors.shuffled(random).take(3) + correct).shuffled(random)
    }
}
