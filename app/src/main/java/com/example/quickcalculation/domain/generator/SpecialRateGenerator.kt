package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object SpecialRateGenerator {

    fun intervalRate(r1: Double, r2: Double): Double = r1 + r2 + r1 * r2
    fun mixedRate(b1: Double, r1: Double, b2: Double, r2: Double): Double =
        (b1 * r1 + b2 * r2) / (b1 + b2)

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        return if (random.nextBoolean()) {
            // 间隔增长率：r = r1 + r2 + r1·r2
            // Ruling 2: clamp rates to a positive lower bound (0.005). sampleRate can return 0.0 for
            // low-growth topics (常住人口 growthRange 0.0..0.02), which would collapse the answer to 0
            // and violate correctAnswer > 0.0.
            val r1 = GenerationUtil.sampleRate(topic, difficulty, random).coerceAtLeast(0.005)
            val r2 = GenerationUtil.sampleRate(topic, difficulty, random).coerceAtLeast(0.005)
            val answer = intervalRate(r1, r2)
            val stem = "${year - 2}年${topic.name}增长${NumberUtil.formatPercent(r1)}，${year - 1}年增长${NumberUtil.formatPercent(r2)}，求${year - 2}年—${year}年${topic.name}的间隔增长率。"
            val explanation = "间隔增长率 = r₁ + r₂ + r₁×r₂ = ${NumberUtil.formatPercent(r1)} + ${NumberUtil.formatPercent(r2)} + ${NumberUtil.formatPercent(r1)}×${NumberUtil.formatPercent(r2)} = ${NumberUtil.formatPercent(answer)}"
            // Ruling 1: unit "%" stores percentage points, so scale the fractional answer by 100.
            // Ruling 3: explanation keeps formatPercent(...) rendering "%" as-is; pure function unchanged.
            Question(QuestionType.SPECIAL_RATE, "间隔增长率", topic.name, stem, GenerationUtil.roundForDifficulty(answer * 100.0, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer * 100.0, difficulty), difficulty, random,
                    listOf((r1 + r2) * 100, r2 * 100, (r1 + r2 + r2 * r2) * 100)), "%", explanation, difficulty)
        } else {
            // 混合增长率：r = (B₁r₁ + B₂r₂) ÷ (B₁ + B₂)
            val b1 = GenerationUtil.sampleBase(topic, difficulty, random)
            val b2 = GenerationUtil.sampleBase(topic, difficulty, random)
            // Ruling 2: positive lower bound on rates (see 间隔 branch comment).
            val r1 = GenerationUtil.sampleRate(topic, difficulty, random).coerceAtLeast(0.005)
            val r2 = GenerationUtil.sampleRate(topic, difficulty, random).coerceAtLeast(0.005)
            val answer = mixedRate(b1, r1, b2, r2)
            val stem = "${year}年${topic.name}中，A部分为${NumberUtil.format(b1, dec)}${topic.unit}（增速${NumberUtil.formatPercent(r1)}），B部分为${NumberUtil.format(b2, dec)}${topic.unit}（增速${NumberUtil.formatPercent(r2)}），求整体混合增长率。"
            val explanation = "混合增长率 = (B₁r₁ + B₂r₂) ÷ (B₁ + B₂) = (${NumberUtil.format(b1, dec)}×${NumberUtil.formatPercent(r1)} + ${NumberUtil.format(b2, dec)}×${NumberUtil.formatPercent(r2)}) ÷ ${NumberUtil.format(b1 + b2, dec)} = ${NumberUtil.formatPercent(answer)}"
            // Ruling 1: scale the fractional answer by 100 for the "%" unit.
            Question(QuestionType.SPECIAL_RATE, "混合增长率", topic.name, stem, GenerationUtil.roundForDifficulty(answer * 100.0, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer * 100.0, difficulty), difficulty, random,
                    listOf((r1 + r2) / 2 * 100, (b1 * r2 + b2 * r1) / (b1 + b2) * 100, maxOf(r1, r2) * 100)), "%", explanation, difficulty)
        }
    }
}
