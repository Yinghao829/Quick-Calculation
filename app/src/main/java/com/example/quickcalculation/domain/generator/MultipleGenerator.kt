package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.random.Random

object MultipleGenerator {

    fun presentMultiple(a: Double, b: Double): Double = a / b
    fun baseMultiple(a: Double, b: Double, aRate: Double, bRate: Double): Double =
        (a / (1 + aRate)) / (b / (1 + bRate))

    fun generate(difficulty: Difficulty, random: Random): Question {
        val topic = TopicRepository.randomValueTopic(random)
        val b = GenerationUtil.sampleBase(topic, difficulty, random) // 较小者
        val m = listOf(1.5, 2.0, 2.5, 3.0, 4.0)[random.nextInt(5)]
        val a = GenerationUtil.roundForDifficulty(b * m, difficulty) // 较大者
        val aRate = GenerationUtil.sampleRate(topic, difficulty, random)
        val bRate = GenerationUtil.sampleRate(topic, difficulty, random)
        val dec = GenerationUtil.decimals(difficulty)
        val year = GenerationUtil.year(random)

        return if (random.nextBoolean()) {
            val answer = presentMultiple(a, b)
            val stem = "${year}年A地区${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}，B地区为${NumberUtil.format(b, dec)}${topic.unit}，求A是B的多少倍。"
            val explanation = "现期倍数 = A ÷ B = ${NumberUtil.format(a, dec)} ÷ ${NumberUtil.format(b, dec)} = ${NumberUtil.format(answer, dec)}倍"
            Question(QuestionType.MULTIPLE, "现期倍数", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "倍", explanation, difficulty)
        } else {
            val answer = baseMultiple(a, b, aRate, bRate)
            val stem = "${year}年A地区${topic.name}为${NumberUtil.format(a, dec)}${topic.unit}（增速${NumberUtil.formatPercent(aRate)}），B地区为${NumberUtil.format(b, dec)}${topic.unit}（增速${NumberUtil.formatPercent(bRate)}），求基期倍数。"
            val explanation = "基期倍数 = (A÷(1+a)) ÷ (B÷(1+b)) = (${NumberUtil.format(a, dec)}÷${NumberUtil.format(1 + aRate, dec)}) ÷ (${NumberUtil.format(b, dec)}÷${NumberUtil.format(1 + bRate, dec)}) = ${NumberUtil.format(answer, dec)}倍"
            Question(QuestionType.MULTIPLE, "基期倍数", topic.name, stem, GenerationUtil.roundForDifficulty(answer, difficulty),
                OptionGenerator.build(GenerationUtil.roundForDifficulty(answer, difficulty), difficulty, random), "倍", explanation, difficulty)
        }
    }
}
