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

    /** 增长因子 (1+rate) 展示：固定 3 位小数，保证千分位精度增长率（如 0.125 → 1.125）不丢失。 */
    fun formatFactor(rate: Double): String = format(1 + rate, 3)

    /** 增速的题干/解析措辞：正→「增长X%」，负→「下降X%」（X 取绝对值）。 */
    fun formatTrend(rate: Double): String =
        if (rate < 0) "下降${formatPercent(abs(rate))}" else "增长${formatPercent(rate)}"

    fun withinTolerance(user: Double, correct: Double, tolerance: Double): Boolean {
        if (correct == 0.0) return abs(user - correct) <= tolerance
        return abs(user - correct) / abs(correct) <= tolerance
    }
}
