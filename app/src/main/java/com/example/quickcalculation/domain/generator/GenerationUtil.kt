package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Topic
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object GenerationUtil {

    fun decimals(difficulty: Difficulty): Int = PrecisionPolicy.decimals(difficulty)

    fun roundForDifficulty(value: Double, difficulty: Difficulty): Double =
        NumberUtil.round(value, decimals(difficulty))

    fun sampleBase(topic: Topic, difficulty: Difficulty, random: Random): Double {
        val raw = random.nextDouble(topic.magnitude.start, topic.magnitude.endInclusive)
        var v = NumberUtil.round(raw, decimals(difficulty))
        if (difficulty == Difficulty.EASY) {
            v = NumberUtil.round(v / 10.0, 0) * 10.0 // 整十，便于手算
        }
        return if (v <= 0.0) topic.magnitude.start else v
    }

    fun sampleRate(topic: Topic, difficulty: Difficulty, random: Random): Double =
        GrowthRatePool.sample(difficulty, random, topic.growthRange)

    fun minimumDisplayableRate(difficulty: Difficulty): Double =
        GrowthRatePool.minimumDisplayableRate(difficulty)

    fun formatRate(rate: Double, difficulty: Difficulty): String =
        NumberUtil.formatPercent(rate, GrowthRatePool.percentDecimals(difficulty))

    fun formatTrend(rate: Double, difficulty: Difficulty): String =
        if (rate < 0) "下降${formatRate(-rate, difficulty)}" else "增长${formatRate(rate, difficulty)}"

    fun formatFactor(rate: Double, difficulty: Difficulty): String =
        NumberUtil.format(1 + rate, GrowthRatePool.rateDecimals(difficulty))

    fun year(random: Random): Int = 2020 + random.nextInt(6)
}
