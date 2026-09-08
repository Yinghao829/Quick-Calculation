package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Topic

/**
 * 基期/增长关系原语：以干净基期 B 与增长率 r 为种子，派生现期 A、增长量 X，
 * 保证 A = B×(1+r)、X = A−B、r = X÷B 恒成立。
 */
data class BaseGrowth(
    val topic: Topic,
    val baseValue: Double,
    val growthRate: Double,
) {
    val currentValue: Double get() = baseValue * (1 + growthRate)
    val growthAmount: Double get() = currentValue - baseValue
}
