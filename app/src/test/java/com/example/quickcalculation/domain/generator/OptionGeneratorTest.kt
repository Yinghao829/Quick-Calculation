package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionGeneratorTest {
    @Test
    fun build_returnsFourDistinctOptionsWithCorrect() {
        val rnd = Random(9)
        repeat(500) {
            val correct = 200.0 + it
            val opts = OptionGenerator.build(correct, Difficulty.EASY, rnd)
            assertEquals(4, opts.size)
            assertEquals(4, opts.distinct().size)
            assertTrue(correct in opts)
        }
    }

    @Test
    fun build_positiveDistractorsForPositiveCorrect() {
        val rnd = Random(11)
        repeat(500) {
            val opts = OptionGenerator.build(120.0, Difficulty.MEDIUM, rnd)
            assertTrue(opts.all { it > 0.0 })
        }
    }

    @Test
    fun build_negativeCorrectAllowed() {
        val rnd = Random(13)
        val opts = OptionGenerator.build(-0.10, Difficulty.HARD, rnd)
        assertEquals(4, opts.distinct().size)
        assertTrue((-0.10) in opts)
    }

    @Test
    fun build_zeroCorrect_returnsFourDistinctOptions() {
        val opts = OptionGenerator.build(0.0, Difficulty.EASY, Random(1))
        assertEquals(4, opts.size)
        assertEquals(4, opts.distinct().size)
        assertTrue(0.0 in opts)
    }

    @Test
    fun build_usesDistractorsAndKeepsInvariants() {
        val rnd = Random(5)
        val opts = OptionGenerator.build(500.0, Difficulty.EASY, rnd, listOf(480.0, 100.0, 550.0))
        assertEquals(4, opts.size)
        assertEquals(4, opts.distinct().size)
        assertTrue(500.0 in opts)
        assertTrue(opts.all { it > 0.0 })
    }

    @Test
    fun build_correctPositionRoughlyBalanced() {
        val rnd = Random(3)
        val counts = IntArray(4)
        repeat(500) {
            val opts = OptionGenerator.build(100.0, Difficulty.MEDIUM, rnd)
            counts[opts.indexOf(100.0)]++
        }
        counts.forEachIndexed { idx, count -> assertTrue("位置 $idx 出现 $count/500 过少", count > 50) }
    }
}
