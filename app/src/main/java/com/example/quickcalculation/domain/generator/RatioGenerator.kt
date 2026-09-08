package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.math.abs
import kotlin.random.Random

object RatioGenerator {

    fun presentRatio(p: Double, q: Double): Double = p / q
    fun baseRatio(p: Double, q: Double, a: Double, b: Double): Double = (p / q) * (1 + b) / (1 + a)
    fun gapRatio(p: Double, q: Double, a: Double, b: Double): Double = (p / q) * (a - b) / (1 + a)

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val q = GenerationUtil.sampleBase(topic, difficulty, random) // 整体现期
        val a = GenerationUtil.sampleRate(topic, difficulty, random) // 部分增速
        val b = GenerationUtil.sampleRate(topic, difficulty, random) // 整体增速
        val fraction = listOf(0.2, 0.25, 0.3, 0.4, 0.5, 0.6, 0.7)[random.nextInt(7)]
        val p = GenerationUtil.roundForDifficulty(q * fraction, difficulty) // 部分现期
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        val (subType, answer, explanation) = when (random.nextInt(3)) {
            0 -> Triple("现期比重", presentRatio(p, q),
                "现期比重 = 部分现期 ÷ 整体现期 = ${NumberUtil.format(p, dec)} ÷ ${NumberUtil.format(q, dec)} = ${NumberUtil.formatPercent(presentRatio(p, q))}")
            1 -> Triple("基期比重", baseRatio(p, q, a, b),
                "基期比重 = (P/Q) × (1+b) ÷ (1+a) = ${NumberUtil.formatPercent(presentRatio(p, q))} × ${NumberUtil.format(1 + b, dec)} ÷ ${NumberUtil.format(1 + a, dec)} = ${NumberUtil.formatPercent(baseRatio(p, q, a, b))}")
            else -> {
                // 两期比重差可为负（部分增速低于整体）；V1 仅出正差距题（与增长量任务舍弃「减少量」同理），
                // 并对接近 0 的差距取一个下限，避免 roundForDifficulty 后坍缩为 0。
                val gap = abs(gapRatio(p, q, a, b))
                Triple("两期比重差", gap,
                    "两期比重差 = (P/Q) × |a−b| ÷ (1+a) = ${NumberUtil.formatPercent(presentRatio(p, q))} × ${NumberUtil.formatPercent(abs(a - b))} ÷ ${NumberUtil.format(1 + a, dec)} = ${NumberUtil.formatPercent(gap)}")
            }
        }
        // 比重/比重差天然落在小数区间（0~1），选择题按百分比（%）作答，故答案以百分点计（×100）。
        // 现期/基期比重的最小 fraction 为 0.2，×100 后不会被 roundForDifficulty 舍为 0；
        // 两期比重差则需下限，保证舍入后仍 > 0。
        val halfStep = when (difficulty) {
            Difficulty.EASY -> 0.5
            Difficulty.MEDIUM -> 0.05
            Difficulty.HARD -> 0.005
        }
        val rawAnswerPct = if (subType == "两期比重差") maxOf(answer * 100.0, halfStep) else answer * 100.0
        val answerRounded = GenerationUtil.roundForDifficulty(rawAnswerPct, difficulty)
        val unit = if (subType == "两期比重差") "个百分点" else "%"
        val stem = "${year}年${topic.name}中，某部分为${NumberUtil.format(p, dec)}${topic.unit}，整体为${NumberUtil.format(q, dec)}${topic.unit}，部分增速${NumberUtil.formatPercent(a)}，整体增速${NumberUtil.formatPercent(b)}，求${subType}。"
        return Question(QuestionType.RATIO, subType, topic.name, stem, answerRounded,
            OptionGenerator.build(answerRounded, difficulty, random), unit, explanation, difficulty)
    }
}
