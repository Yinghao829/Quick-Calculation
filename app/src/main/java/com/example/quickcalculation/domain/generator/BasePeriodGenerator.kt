package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object BasePeriodGenerator {

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val b = GenerationUtil.sampleBase(topic, difficulty, random)
        val r = GenerationUtil.sampleRate(topic, difficulty, random)
        val bg = BaseGrowth(topic, b, r)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        return if (random.nextBoolean()) {
            // 求基期量：已知现期 A 与增长率 r，求 B = A ÷ (1+r)
            val answer = GenerationUtil.roundForDifficulty(bg.baseValue, difficulty)
            val a = GenerationUtil.roundForDifficulty(bg.currentValue, difficulty)
            val stem = "${year}年${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，同比增长${NumberUtil.formatPercent(r)}，求基期${topic.name}。"
            val explanation = "基期量 = 现期量 ÷ (1 + 增长率) = ${NumberUtil.format(a, dec)} ÷ ${NumberUtil.formatFactor(r)} = ${NumberUtil.format(answer, dec)}${topic.unit}"
            Question(
                type = QuestionType.BASE_PERIOD, subType = "求基期量", topic = topic.name,
                stem = stem, correctAnswer = answer,
                options = OptionGenerator.build(answer, difficulty, random, listOf(a * (1 - r))),
                unit = topic.unit, explanation = explanation, difficulty = difficulty,
            )
        } else {
            // 求现期量：已知基期 B 与增长率 r，求 A = B × (1+r)
            val answer = GenerationUtil.roundForDifficulty(bg.currentValue, difficulty)
            val stem = "${year}年${topic.name}为${NumberUtil.format(b, dec)}${topic.unit}，同比增长${NumberUtil.formatPercent(r)}，求现期${topic.name}。"
            val explanation = "现期量 = 基期量 × (1 + 增长率) = ${NumberUtil.format(b, dec)} × ${NumberUtil.formatFactor(r)} = ${NumberUtil.format(answer, dec)}${topic.unit}"
            Question(
                type = QuestionType.BASE_PERIOD, subType = "求现期量", topic = topic.name,
                stem = stem, correctAnswer = answer,
                options = OptionGenerator.build(answer, difficulty, random, listOf(b * (1 + r / 2))),
                unit = topic.unit, explanation = explanation, difficulty = difficulty,
            )
        }
    }
}
