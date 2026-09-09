package com.example.quickcalculation.ui

import com.example.quickcalculation.domain.util.NumberUtil

object AnswerFormatter {
    fun format(value: Double, unit: String): String =
        NumberUtil.format(value, decimals = if (value % 1.0 == 0.0) 0 else 2) + unit
}
