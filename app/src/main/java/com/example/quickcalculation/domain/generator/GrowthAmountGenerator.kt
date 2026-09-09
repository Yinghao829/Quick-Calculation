package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.math.abs
import kotlin.random.Random

object GrowthAmountGenerator {

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val b = GenerationUtil.sampleBase(topic, difficulty, random)
        // 增长量为正数：负增长属于「减少量」、零增长无答案，均不在本子类型范围内。
        // 故取 |r| 并使用难度对应的最小可展示增长率，保证舍入后 correct > 0。
        val r = maxOf(abs(GenerationUtil.sampleRate(topic, difficulty, random)), GenerationUtil.minimumDisplayableRate(difficulty))
        val bg = BaseGrowth(topic, b, r)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)
        val a = GenerationUtil.roundForDifficulty(bg.currentValue, difficulty)

        return if (random.nextBoolean()) {
            // X = A − B
            val answer = GenerationUtil.roundForDifficulty(bg.growthAmount, difficulty)
            val stem = "${year}年${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，上年为${NumberUtil.format(b, dec)}${topic.unit}，求增长量。"
            val explanation = "增长量 = 现期量 − 基期量 = ${NumberUtil.format(a, dec)} − ${NumberUtil.format(b, dec)} = ${NumberUtil.format(answer, dec)}${topic.unit}"
            Question(QuestionType.GROWTH_AMOUNT, "求增长量(现期+基期)", topic.name, stem, answer,
                OptionGenerator.build(answer, difficulty, random, listOf(b, a, a * r)), topic.unit, explanation, difficulty)
        } else {
            // X = A·r ÷ (1+r)
            val answer = GenerationUtil.roundForDifficulty(bg.currentValue * bg.growthRate / (1 + bg.growthRate), difficulty)
            val stem = "${year}年${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，同比增长${GenerationUtil.formatRate(r, difficulty)}，求增长量。"
            val explanation = "增长量 = 现期量 × 增长率 ÷ (1 + 增长率) = ${NumberUtil.format(a, dec)} × ${GenerationUtil.formatRate(r, difficulty)} ÷ ${GenerationUtil.formatFactor(r, difficulty)} = ${NumberUtil.format(answer, dec)}${topic.unit}"
            Question(QuestionType.GROWTH_AMOUNT, "求增长量(现期+增长率)", topic.name, stem, answer,
                OptionGenerator.build(answer, difficulty, random, listOf(a * r, a, a * r / (1 - r))), topic.unit, explanation, difficulty)
        }
    }
}
