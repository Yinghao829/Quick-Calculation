package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import kotlin.random.Random

/** 增长率池：按难度加权采样，常见数值出现概率更高。每个条目为 (rate, weight)。 */
object GrowthRatePool {
    private val easy = listOf(
        0.05 to 3, 0.10 to 3, 0.20 to 2, 0.25 to 2,
    )
    private val medium = listOf(
        0.05 to 2, 0.08 to 2, 0.10 to 3, 0.125 to 2, 0.15 to 2, 0.20 to 3, 0.25 to 2,
    )
    private val hard = listOf(
        -0.20 to 1, -0.15 to 1, -0.125 to 1, -0.10 to 1, -0.08 to 1, -0.05 to 1,
        0.075 to 1, 0.125 to 2, 0.15 to 2, 0.175 to 1, 0.18 to 1, 0.20 to 2, 0.225 to 1, 0.25 to 2,
    )

    fun sample(difficulty: Difficulty, random: Random): Double {
        val pool = pool(difficulty)
        val total = pool.sumOf { it.second }
        var idx = random.nextInt(total)
        for ((rate, weight) in pool) {
            if (idx < weight) return rate
            idx -= weight
        }
        return pool.last().first
    }

    fun poolFor(difficulty: Difficulty): List<Double> = pool(difficulty).map { it.first }

    private fun pool(difficulty: Difficulty): List<Pair<Double, Int>> = when (difficulty) {
        Difficulty.EASY -> easy
        Difficulty.MEDIUM -> medium
        Difficulty.HARD -> hard
    }
}
