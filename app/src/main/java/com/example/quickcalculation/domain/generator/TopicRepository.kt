package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Topic
import kotlin.random.Random

object TopicRepository {
    val topics: List<Topic> = listOf(
        Topic("国内生产总值", "亿元", 80000.0..150000.0, 0.02..0.08),
        Topic("社会消费品零售总额", "亿元", 3000.0..60000.0, 0.03..0.10),
        Topic("进出口总额", "亿美元", 2000.0..30000.0, -0.10..0.15),
        Topic("规模以上工业增加值", "%", 3.0..9.0, 0.02..0.08, isRateType = true),
        Topic("固定资产投资", "亿元", 1000.0..50000.0, -0.05..0.10),
        Topic("居民人均可支配收入", "元", 20000.0..60000.0, 0.03..0.08),
        Topic("常住人口", "万人", 500.0..3000.0, 0.0..0.02),
        Topic("居民消费价格", "%", 0.0..3.0, 0.0..0.03, isRateType = true),
    )

    fun randomValueTopic(random: Random): Topic {
        val valueTopics = topics.filter { !it.isRateType }
        return valueTopics[random.nextInt(valueTopics.size)]
    }
}
