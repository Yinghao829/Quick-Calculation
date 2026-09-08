package com.example.quickcalculation.domain.model

/**
 * 题材定义。`isRateType` 为 true 表示该题材的「值」本身就是一个增速（如 CPI、工业增加值增速），
 * 仅用于增速类题目；数值类题目通过 [TopicRepository.randomValueTopic] 过滤掉。
 */
data class Topic(
    val name: String,
    val unit: String,
    val magnitude: ClosedRange<Double>,
    val growthRange: ClosedRange<Double>,
    val isRateType: Boolean = false,
)
