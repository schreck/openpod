package com.openpod.ui.episodes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeProgressTest {

    @Test
    fun `H_MM_SS format`() {
        assertEquals(5025000L, parseDurationMs("1:23:45"))
    }

    @Test
    fun `MM_SS format`() {
        assertEquals(2700000L, parseDurationMs("45:00"))
    }

    @Test
    fun `plain seconds format`() {
        assertEquals(3600000L, parseDurationMs("3600"))
    }

    @Test
    fun `zero duration`() {
        assertEquals(0L, parseDurationMs("0:00:00"))
        assertEquals(0L, parseDurationMs("0:00"))
        assertEquals(0L, parseDurationMs("0"))
    }

    @Test
    fun `null returns null`() {
        assertNull(parseDurationMs(null))
    }

    @Test
    fun `blank returns null`() {
        assertNull(parseDurationMs(""))
        assertNull(parseDurationMs("   "))
    }

    @Test
    fun `non-numeric returns null`() {
        assertNull(parseDurationMs("one hour"))
        assertNull(parseDurationMs("1:xx:00"))
        assertNull(parseDurationMs("abc:def"))
    }

    @Test
    fun `extra colons returns null`() {
        assertNull(parseDurationMs("1:2:3:4"))
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertEquals(60000L, parseDurationMs("  1:00  "))
    }
}
