package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Topic
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random
import kotlin.math.abs

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

    fun sampleRate(topic: Topic, difficulty: Difficulty, random: Random): Double {
        // 优先从难度池取（保证可手算），但必须落在题材合理区间内（R5）。
        repeat(40) {
            val r = GrowthRatePool.sample(difficulty, random)
            if (r in topic.growthRange) return r
        }
        // 低增速题材（人口/CPI）：池内无满足值，改用 0.5% 步长的干净值。
        val lo = topic.growthRange.start
        val hi = topic.growthRange.endInclusive
        val step = 0.005
        val steps = ((hi - lo) / step).toInt().coerceAtLeast(1)
        val r = lo + random.nextInt(steps + 1) * step
        return r.coerceAtMost(hi)
    }

    fun year(random: Random): Int = 2020 + random.nextInt(6)
}
