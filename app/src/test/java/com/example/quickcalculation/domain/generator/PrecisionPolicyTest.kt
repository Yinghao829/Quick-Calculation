package com.example.quickcalculation.domain.generator

import com.example.quickcalculation.domain.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class PrecisionPolicyTest {
    @Test
    fun decimals_matchesDifficultyTiers() {
        assertEquals(0, PrecisionPolicy.decimals(Difficulty.EASY))
        assertEquals(1, PrecisionPolicy.decimals(Difficulty.MEDIUM))
        assertEquals(2, PrecisionPolicy.decimals(Difficulty.HARD))
    }

    @Test
    fun generationUtil_decimalsDelegatesToPolicy() {
        assertEquals(PrecisionPolicy.decimals(Difficulty.EASY), GenerationUtil.decimals(Difficulty.EASY))
        assertEquals(PrecisionPolicy.decimals(Difficulty.MEDIUM), GenerationUtil.decimals(Difficulty.MEDIUM))
        assertEquals(PrecisionPolicy.decimals(Difficulty.HARD), GenerationUtil.decimals(Difficulty.HARD))
    }
}
