package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.math.pow
import kotlin.random.Random

/** 连续增长率采样器：题材区间是硬边界，分段权重控制小幅变化更常见。 */
object GrowthRatePool {
    private data class Band(
        val lowerMagnitude: Double,
        val upperMagnitude: Double,
        val easyWeight: Double,
        val mediumWeight: Double,
        val hardWeight: Double,
    ) {
        fun weight(difficulty: Difficulty): Double = when (difficulty) {
            Difficulty.EASY -> easyWeight
            Difficulty.MEDIUM -> mediumWeight
            Difficulty.HARD -> hardWeight
        }
    }

    private data class CandidateInterval(
        val sign: Int,
        val lowerMagnitude: Double,
        val upperMagnitude: Double,
        val weight: Double,
    )

    private val bands = listOf(
        Band(0.0, 0.10, easyWeight = 88.0, mediumWeight = 76.0, hardWeight = 70.0),
        Band(0.10, 0.50, easyWeight = 11.0, mediumWeight = 21.0, hardWeight = 24.0),
        Band(0.50, 1.00, easyWeight = 1.0, mediumWeight = 3.0, hardWeight = 5.0),
        Band(1.00, Double.POSITIVE_INFINITY, easyWeight = 0.0, mediumWeight = 0.0, hardWeight = 1.0),
    )

    fun sample(difficulty: Difficulty, random: Random): Double =
        sample(difficulty, random, DEFAULT_RANGE)

    fun sample(difficulty: Difficulty, random: Random, growthRange: ClosedRange<Double>): Double {
        val lo = maxOf(growthRange.start, minimumLegalRate(difficulty))
        val hi = growthRange.endInclusive
        require(lo <= hi) { "growthRange must contain at least one rate greater than -100%" }

        if (lo == hi) return quantize(lo, difficulty, lo, hi)

        val intervals = candidateIntervals(lo, hi, difficulty)
        if (intervals.isEmpty()) {
            return quantize(random.nextDouble(lo, hi), difficulty, lo, hi)
        }

        val selected = chooseInterval(intervals, random)
        val u = random.nextDouble().pow(bias(difficulty))
        val magnitude = selected.lowerMagnitude + (selected.upperMagnitude - selected.lowerMagnitude) * u
        return quantize(selected.sign * magnitude, difficulty, lo, hi)
    }

    fun rateDecimals(difficulty: Difficulty): Int = percentDecimals(difficulty) + 2

    fun percentDecimals(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 0
        Difficulty.MEDIUM -> 1
        Difficulty.HARD -> 2
    }

    fun minimumDisplayableRate(difficulty: Difficulty): Double = when (difficulty) {
        Difficulty.EASY -> 0.01
        Difficulty.MEDIUM -> 0.001
        Difficulty.HARD -> 0.0001
    }

    private fun candidateIntervals(lo: Double, hi: Double, difficulty: Difficulty): List<CandidateInterval> {
        val directionWeights = directionWeights(lo, hi)
        val intervals = buildList {
            if (hi > 0.0) {
                addIntervalsForDirection(
                    sign = 1,
                    magnitudeStart = maxOf(lo, 0.0),
                    magnitudeEnd = hi,
                    directionWeight = directionWeights.first,
                    difficulty = difficulty,
                )
            }
            if (lo < 0.0) {
                addIntervalsForDirection(
                    sign = -1,
                    magnitudeStart = if (hi < 0.0) -hi else 0.0,
                    magnitudeEnd = -lo,
                    directionWeight = directionWeights.second,
                    difficulty = difficulty,
                )
            }
        }
        if (intervals.any { it.weight > 0.0 }) return intervals.filter { it.weight > 0.0 }
        return intervals.map { it.copy(weight = 1.0) }
    }

    private fun MutableList<CandidateInterval>.addIntervalsForDirection(
        sign: Int,
        magnitudeStart: Double,
        magnitudeEnd: Double,
        directionWeight: Double,
        difficulty: Difficulty,
    ) {
        if (magnitudeEnd <= magnitudeStart) return
        for (band in bands) {
            val lower = maxOf(magnitudeStart, band.lowerMagnitude)
            val upper = minOf(magnitudeEnd, band.upperMagnitude)
            if (upper <= lower) continue

            val coverage = if (band.upperMagnitude.isFinite()) {
                (upper - lower) / (band.upperMagnitude - band.lowerMagnitude)
            } else {
                1.0
            }
            add(CandidateInterval(sign, lower, upper, band.weight(difficulty) * coverage * directionWeight))
        }
    }

    private fun directionWeights(lo: Double, hi: Double): Pair<Double, Double> = when {
        lo >= 0.0 -> 1.0 to 0.0
        hi <= 0.0 -> 0.0 to 1.0
        else -> 0.75 to 0.25
    }

    private fun chooseInterval(intervals: List<CandidateInterval>, random: Random): CandidateInterval {
        val total = intervals.sumOf { it.weight }
        var point = random.nextDouble(total)
        for (interval in intervals) {
            if (point < interval.weight) return interval
            point -= interval.weight
        }
        return intervals.last()
    }

    private fun quantize(rate: Double, difficulty: Difficulty, lo: Double, hi: Double): Double {
        val rounded = NumberUtil.round(rate, rateDecimals(difficulty))
        return rounded.coerceIn(lo, hi).coerceAtLeast(minimumLegalRate(difficulty))
    }

    private fun minimumLegalRate(difficulty: Difficulty): Double =
        -1.0 + minimumDisplayableRate(difficulty)

    private fun bias(difficulty: Difficulty): Double = when (difficulty) {
        Difficulty.EASY -> 2.4
        Difficulty.MEDIUM -> 1.9
        Difficulty.HARD -> 1.55
    }

    private val DEFAULT_RANGE = -0.50..0.50
}
