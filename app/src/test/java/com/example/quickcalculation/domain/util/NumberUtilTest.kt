package com.example.quickcalculation.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberUtilTest {
    @Test
    fun round_halfUp() {
        assertEquals(1.23, NumberUtil.round(1.234, 2), 0.0)
        assertEquals(1.24, NumberUtil.round(1.235, 2), 0.0)
        assertEquals(200.0, NumberUtil.round(200.0, 0), 0.0)
    }

    @Test
    fun format_stripsTrailingZeros() {
        assertEquals("20", NumberUtil.format(20.0, 1))
        assertEquals("12.5", NumberUtil.format(12.5, 1))
        assertEquals("0.5", NumberUtil.format(0.5, 1))
    }

    @Test
    fun formatPercent() {
        assertEquals("20%", NumberUtil.formatPercent(0.20))
        assertEquals("12.5%", NumberUtil.formatPercent(0.125))
        assertEquals("5%", NumberUtil.formatPercent(0.05))
    }

    @Test
    fun formatFactor_preservesRatePrecision() {
        assertEquals("1.05", NumberUtil.formatFactor(0.05))
        assertEquals("1.125", NumberUtil.formatFactor(0.125))
        assertEquals("1.175", NumberUtil.formatFactor(0.175))
        assertEquals("1.1", NumberUtil.formatFactor(0.10))
        assertEquals("0.8", NumberUtil.formatFactor(-0.20))
    }

    @Test
    fun withinTolerance_relative() {
        assertTrue(NumberUtil.withinTolerance(100.4, 100.0, 0.005))
        assertFalse(NumberUtil.withinTolerance(101.0, 100.0, 0.005))
        assertTrue(NumberUtil.withinTolerance(0.0, 0.0, 0.005))
    }
}
