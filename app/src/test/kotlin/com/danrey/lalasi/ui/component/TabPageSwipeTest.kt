package com.danrey.lalasi.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TabPageSwipeTest {
    @Test
    fun elasticIsSymmetricAndZeroAtRest() {
        assertEquals(0f, elastic(0f, 100f), 0f)
        assertEquals(-elastic(40f, 100f), elastic(-40f, 100f), 1e-4f)
    }

    @Test
    fun elasticNeverPassesItsLimit() {
        assertTrue(abs(elastic(100_000f, 100f)) <= 100f)
    }

    @Test
    fun elasticIsMonotonic() {
        var last = 0f
        for (x in 1..500) {
            val v = elastic(x.toFloat(), 100f)
            assertTrue(v >= last)
            last = v
        }
    }

    @Test
    fun elasticStartsCloseToOneToOne() {
        assertEquals(1f, elastic(1f, 300f), 0.01f)
    }
}
