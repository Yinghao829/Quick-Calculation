package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Topic
import com.example.quickcalculation.domain.util.NumberUtil
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfrastructureTest {

    @Test
    fun continuousGrowthRate_staysInEveryTopicGrowthRange() {
        val rnd = Random(7)
        for (topic in TopicRepository.topics) {
            for (difficulty in Difficulty.entries) {
                repeat(1_000) {
                    val r = GenerationUtil.sampleRate(topic, difficulty, rnd)
                    assertTrue("${topic.name} $difficulty generated $r outside ${topic.growthRange}", r in topic.growthRange)
                    assertTrue("growth rate must stay greater than -100%, actual $r", r > -1.0)
                    assertQuantized(r, difficulty)
                }
            }
        }
    }

    @Test
    fun continuousGrowthRate_positiveOnlyTopicsDoNotGenerateDecline() {
        val rnd = Random(11)
        val positiveOnlyTopics = TopicRepository.topics.filter { it.growthRange.start >= 0.0 }
        assertTrue(positiveOnlyTopics.isNotEmpty())

        for (topic in positiveOnlyTopics) {
            repeat(1_000) {
                val r = GenerationUtil.sampleRate(topic, Difficulty.HARD, rnd)
                assertTrue("${topic.name} should not generate decline, actual $r", r >= 0.0)
            }
        }
    }

    @Test
    fun continuousGrowthRate_negativeCapableRangeGeneratesDeclines() {
        val rnd = Random(23)
        val volatileTopic = Topic("进出口测试", "亿元", 100.0..1000.0, -0.15..0.20)

        val samples = List(5_000) { GenerationUtil.sampleRate(volatileTopic, Difficulty.HARD, rnd) }

        assertTrue("expected some decline samples", samples.any { it < 0.0 })
        assertTrue("expected some growth samples", samples.any { it > 0.0 })
        assertTrue(samples.all { it in volatileTopic.growthRange })
    }

    @Test
    fun continuousGrowthRate_exactRangeAlwaysReturnsThatRate() {
        val topic = Topic("固定边界", "万元", 10.0..100.0, 0.0234..0.0234)

        for (difficulty in Difficulty.entries) {
            repeat(100) {
                assertEquals(0.0234, GenerationUtil.sampleRate(topic, difficulty, Random(it + difficulty.ordinal)), 1e-12)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun continuousGrowthRate_rejectsRangeAtOrBelowMinusOne() {
        val invalidTopic = Topic("非法下降", "万元", 10.0..100.0, -1.50..-1.00)

        GenerationUtil.sampleRate(invalidTopic, Difficulty.HARD, Random(37))
    }

    @Test
    fun continuousGrowthRate_neverReturnsMinusOneOrBelow() {
        val rnd = Random(31)
        val severeDeclineTopic = Topic("严重下降业务", "万元", 10.0..100.0, -1.20..-0.95)

        repeat(1_000) {
            val r = GenerationUtil.sampleRate(severeDeclineTopic, Difficulty.HARD, rnd)
            assertTrue("rate must stay greater than -100%, actual $r", r > -1.0)
            assertTrue(r in severeDeclineTopic.growthRange)
        }
    }

    @Test
    fun continuousGrowthRate_distributionPrefersSmallMagnitudeButKeepsTail() {
        val rnd = Random(41)
        val highVolatilityTopic = Topic("低基数新业务", "万元", 10.0..100.0, -0.50..1.50)

        val samples = List(10_000) { GenerationUtil.sampleRate(highVolatilityTopic, Difficulty.HARD, rnd) }
        val small = samples.count { abs(it) <= 0.10 }
        val medium = samples.count { abs(it) > 0.10 && abs(it) <= 0.50 }
        val large = samples.count { abs(it) > 0.50 && abs(it) <= 1.00 }
        val extreme = samples.count { abs(it) > 1.00 }

        assertTrue("small=$small medium=$medium", small > medium)
        assertTrue("medium=$medium large=$large", medium > large)
        assertTrue("large=$large extreme=$extreme", large > extreme)
        assertTrue("tail samples should not disappear", large > 100)
        assertTrue("extreme samples should remain rare but possible", extreme in 20..300)
    }

    @Test
    fun continuousGrowthRate_hardModeProducesManyIrregularValues() {
        val rnd = Random(53)
        val topic = Topic("不规则测试", "万元", 10.0..100.0, -0.50..0.50)

        val samples = List(2_000) { GrowthRatePool.sample(Difficulty.HARD, rnd, topic.growthRange) }
        val distinct = samples.distinct().size
        val hasTwoDecimalPercent = samples.any { (abs(it) * 10_000).roundToInt() % 10 != 0 }

        assertTrue("expected many distinct values, actual $distinct", distinct > 500)
        assertTrue("expected values beyond one decimal percent", hasTwoDecimalPercent)
    }

    @Test
    fun topicRepository_hasEightTopicsAndFiltersRateTypes() {
        assertEquals(8, TopicRepository.topics.size)
        assertEquals(8, TopicRepository.topics.map { it.name }.distinct().size)
        val rnd = Random(1)
        repeat(100) {
            assertTrue(!TopicRepository.randomValueTopic(rnd).isRateType)
        }
    }

    @Test
    fun baseGrowth_invariantsHold() {
        val topic = TopicRepository.topics.first()
        val bg = BaseGrowth(topic, baseValue = 500.0, growthRate = 0.20)
        assertEquals(600.0, bg.currentValue, 1e-9)
        assertEquals(100.0, bg.growthAmount, 1e-9)
        // r = X / B
        assertEquals(bg.growthRate, bg.growthAmount / bg.baseValue, 1e-9)
        // X = A * r / (1 + r)
        assertEquals(bg.growthAmount, bg.currentValue * bg.growthRate / (1 + bg.growthRate), 1e-9)
    }

    @Test
    fun sampleBase_positive() {
        val rnd = Random(3)
        val topic = TopicRepository.topics.first()
        repeat(100) {
            assertTrue(GenerationUtil.sampleBase(topic, Difficulty.MEDIUM, rnd) > 0.0)
        }
    }

    private fun assertQuantized(rate: Double, difficulty: Difficulty) {
        assertEquals(NumberUtil.round(rate, GrowthRatePool.rateDecimals(difficulty)), rate, 1e-12)
    }
}
