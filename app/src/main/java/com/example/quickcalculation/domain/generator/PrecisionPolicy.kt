package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty

/** 填空题答案精度策略（FILL_IN_QUESTION.md §11）。
 *  中间计算始终保留高精度，仅最终答案按难度舍入到该小数位。 */
object PrecisionPolicy {

    /** EASY 整数 / MEDIUM 1 位 / HARD 2 位。 */
    fun decimals(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> 0
        Difficulty.MEDIUM -> 1
        Difficulty.HARD -> 2
    }
}
