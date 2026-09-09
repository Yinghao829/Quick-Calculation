package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import kotlin.random.Random

/** 增长率池：按难度加权采样，常见数值出现概率更高。每个条目为 (rate, weight)。 */
object GrowthRatePool {
    private val easy = listOf(
        0.05 to 4, 0.10 to 4, 0.15 to 3, 0.20 to 3, 0.25 to 2, 0.08 to 2,
        -0.05 to 1, -0.10 to 1,
    )
    private val medium = listOf(
        0.05 to 3, 0.08 to 2, 0.10 to 4, 0.125 to 2, 0.15 to 3, 0.20 to 3, 0.25 to 2,
        0.023 to 2, 0.034 to 2, 0.051 to 2, 0.091 to 2, 0.114 to 1, 0.136 to 1, 0.162 to 1,
        -0.03 to 2, -0.05 to 2, -0.08 to 2, -0.10 to 1,
    )
    private val hard = listOf(
        0.075 to 1, 0.125 to 2, 0.15 to 2, 0.175 to 1, 0.18 to 1, 0.20 to 2, 0.225 to 1, 0.25 to 2,
        0.009 to 1, 0.023 to 1, 0.034 to 1, 0.051 to 1, 0.053 to 1, 0.067 to 1, 0.091 to 1, 0.096 to 1,
        0.112 to 1, 0.142 to 1, 0.168 to 1, 0.213 to 1, 0.238 to 1,
        -0.02 to 1, -0.035 to 1, -0.05 to 2, -0.08 to 2, -0.12 to 1, -0.15 to 1, -0.18 to 1, -0.20 to 1,
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
