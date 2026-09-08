package com.example.quickcalculation.domain.model

enum class QuestionType(val label: String) {
    BASE_PERIOD("基期与现期"),
    GROWTH_AMOUNT("增长量"),
    GROWTH_RATE("一般增长率"),
    RATIO("比重"),
    AVERAGE("平均数"),
    MULTIPLE("倍数"),
    SPECIAL_RATE("特殊增长率"),
}
