package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import com.example.quickcalculation.domain.model.Topic
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfrastructureTest {

    @Test
    fun growthRatePool_sampleAlwaysInPool() {
        val rnd = Random(42)
        repeat(200) {
            val r = GrowthRatePool.sample(Difficulty.EASY, rnd)
            assertTrue(r in GrowthRatePool.poolFor(Difficulty.EASY))
        }
    }

    @Test
    fun growthRatePool_hardIncludesNegative() {
        assertTrue(GrowthRatePool.poolFor(Difficulty.HARD).any { it < 0.0 })
    }

    @Test
    fun growthRatePool_mediumHasIrregularRates() {
        val m = GrowthRatePool.poolFor(Difficulty.MEDIUM)
        assertTrue(m.any { it == 0.023 } && m.any { it == 0.091 } && m.any { it == 0.051 })
    }

    @Test
    fun growthRatePool_easyHasDecline() {
        assertTrue(GrowthRatePool.poolFor(Difficulty.EASY).any { it < 0.0 })
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
    fun sampleRate_staysInTopicGrowthRange() {
        val rnd = Random(7)
        val lowGrowth = Topic("常住人口", "万人", 500.0..3000.0, 0.0..0.02)
        repeat(100) {
            val r = GenerationUtil.sampleRate(lowGrowth, Difficulty.EASY, rnd)
            assertTrue(r in lowGrowth.growthRange)
        }
    }

    @Test
    fun sampleBase_positive() {
        val rnd = Random(3)
        val topic = TopicRepository.topics.first()
        repeat(100) {
            assertTrue(GenerationUtil.sampleBase(topic, Difficulty.MEDIUM, rnd) > 0.0)
        }
    }
}
