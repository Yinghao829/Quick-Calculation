package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Question
import com.example.quickcalculation.domain.model.QuestionType
import kotlin.random.Random

class QuestionGenerator(seed: Long? = null) {
    private val random: Random = seed?.let { Random(it) } ?: Random.Default

    fun generate(type: QuestionType, difficulty: Difficulty): Question = when (type) {
        QuestionType.BASE_PERIOD -> BasePeriodGenerator.generate(difficulty, random)
        QuestionType.GROWTH_AMOUNT -> GrowthAmountGenerator.generate(difficulty, random)
        QuestionType.GROWTH_RATE -> GrowthRateGenerator.generate(difficulty, random)
        QuestionType.RATIO -> RatioGenerator.generate(difficulty, random)
        QuestionType.AVERAGE -> AverageGenerator.generate(difficulty, random)
        QuestionType.MULTIPLE -> MultipleGenerator.generate(difficulty, random)
        QuestionType.SPECIAL_RATE -> SpecialRateGenerator.generate(difficulty, random)
    }

    fun generateRandom(types: List<QuestionType>, difficulty: Difficulty): Question {
        require(types.isNotEmpty()) { "types 不能为空" }
        return generate(types[random.nextInt(types.size)], difficulty)
    }
}
