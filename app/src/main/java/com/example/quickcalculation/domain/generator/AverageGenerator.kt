package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object AverageGenerator {

    private const val PER_CAPITA = 10000.0 // 亿元 ÷ 万人 → 元/人

    fun presentAverage(total: Double, count: Double): Double = total / count * PER_CAPITA
    fun baseAverage(total: Double, count: Double, a: Double, b: Double): Double =
        (total / (1 + a)) / (count / (1 + b)) * PER_CAPITA

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val total = GenerationUtil.sampleBase(topic, difficulty, random)
        val count = GenerationUtil.roundForDifficulty(random.nextDouble(500.0, 5000.0), difficulty)
        val a = GenerationUtil.sampleRate(topic, difficulty, random)
        val b = GenerationUtil.sampleRate(topic, difficulty, random)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        return if (random.nextBoolean()) {
            val answer = presentAverage(total, count)
            val stem = "${year}年${topic.name}总量为${NumberUtil.format(total, dec)}亿元，涉及人数${NumberUtil.format(count, dec)}万人，求现期平均数。"
            val explanation = "现期平均数 = 总量 ÷ 个数 = ${NumberUtil.format(total, dec)} ÷ ${NumberUtil.format(count, dec)} × 10000 = ${NumberUtil.format(answer, dec)}元/人"
            Question(QuestionType.AVERAGE, "现期平均数", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "元", explanation, difficulty)
        } else {
            val answer = baseAverage(total, count, a, b)
            val stem = "${year}年${topic.name}总量为${NumberUtil.format(total, dec)}亿元（增速${NumberUtil.formatPercent(a)}），涉及人数${NumberUtil.format(count, dec)}万人（增速${NumberUtil.formatPercent(b)}），求基期平均数。"
            val explanation = "基期平均数 = (总量÷(1+a)) ÷ (个数÷(1+b)) = (${NumberUtil.format(total, dec)}÷${NumberUtil.format(1 + a, dec)}) ÷ (${NumberUtil.format(count, dec)}÷${NumberUtil.format(1 + b, dec)}) × 10000 = ${NumberUtil.format(answer, dec)}元/人"
            Question(QuestionType.AVERAGE, "基期平均数", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "元", explanation, difficulty)
        }
    }
}
