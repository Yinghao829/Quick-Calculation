package com.example.quickcalculation.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object NumberUtil {

    fun round(value: Double, decimals: Int): Double =
        BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()

    fun format(value: Double, decimals: Int = 2): String =
        BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()

    fun formatPercent(rate: Double, decimals: Int = 1): String =
        "${format(rate * 100, decimals)}%"

    fun withinTolerance(user: Double, correct: Double, tolerance: Double): Boolean {
        if (correct == 0.0) return abs(user - correct) <= tolerance
        return abs(user - correct) / abs(correct) <= tolerance
    }
}
