package com.example.quickcalculation.ui

import com.example.quickcalculation.domain.generator.PrecisionPolicy
import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.util.NumberUtil

object AnswerFormatter {
    /** 按难度精度展示答案/选项，与填空输入的期望精度一致；
     *  未指定难度时退回旧规则（整数不加小数，否则 2 位）。 */
    fun format(value: Double, unit: String, difficulty: Difficulty? = null): String {
        val decimals = difficulty?.let { PrecisionPolicy.decimals(it) }
            ?: if (value % 1.0 == 0.0) 0 else 2
        return NumberUtil.format(value, decimals) + unit
    }
}
