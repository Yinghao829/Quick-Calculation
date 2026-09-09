package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.math.abs
import kotlin.random.Random

object GrowthRateGenerator {

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val b = GenerationUtil.sampleBase(topic, difficulty, random)
        // 一般增长率 V1 只出正增速（下降已在基期现期单独呈现），故取 |r| 并使用最小可展示增长率。
        val r = maxOf(abs(GenerationUtil.sampleRate(topic, difficulty, random)), GenerationUtil.minimumDisplayableRate(difficulty))
        val bg = BaseGrowth(topic, b, r)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)
        val a = GenerationUtil.roundForDifficulty(bg.currentValue, difficulty)
        val answer = GenerationUtil.roundForDifficulty(bg.growthRate * 100.0, difficulty)

        return if (random.nextBoolean()) {
            // r = (A − B) ÷ B
            val stem = "${year}年${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，上年为${NumberUtil.format(b, dec)}${topic.unit}，求增长率。"
            val explanation = "增长率 = (现期量 − 基期量) ÷ 基期量 = (${NumberUtil.format(a, dec)} − ${NumberUtil.format(b, dec)}) ÷ ${NumberUtil.format(b, dec)} = ${GenerationUtil.formatRate(r, difficulty)}"
            Question(QuestionType.GROWTH_RATE, "求增长率(现期+基期)", topic.name, stem, answer,
                OptionGenerator.build(answer, difficulty, random, listOf((a - b) / a * 100, a / b * 100, b / a * 100)), "%", explanation, difficulty)
        } else {
            // r = X ÷ B
            val x = GenerationUtil.roundForDifficulty(bg.growthAmount, difficulty)
            val stem = "${year}年${topic.name}增长量为${NumberUtil.format(x, dec)}${topic.unit}，基期为${NumberUtil.format(b, dec)}${topic.unit}，求增长率。"
            val explanation = "增长率 = 增长量 ÷ 基期量 = ${NumberUtil.format(x, dec)} ÷ ${NumberUtil.format(b, dec)} = ${GenerationUtil.formatRate(r, difficulty)}"
            Question(QuestionType.GROWTH_RATE, "求增长率(增长量+基期)", topic.name, stem, answer,
                OptionGenerator.build(answer, difficulty, random, listOf(x / (b + x) * 100, (b + x) / b * 100, x)), "%", explanation, difficulty)
        }
    }
}
