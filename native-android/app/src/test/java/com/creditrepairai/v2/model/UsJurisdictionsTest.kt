package com.creditrepairai.v2.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsJurisdictionsTest {
    @Test
    fun includesAllStatesAndDistrictOfColumbia() {
        assertEquals(51, UsJurisdictions.names.size)
        assertEquals("District of Columbia", UsJurisdictions.names["DC"])
    }

    @Test
    fun normalizesValidCodesAndRejectsUnknownValues() {
        assertEquals("WA", UsJurisdictions.normalize(" wa "))
        assertNull(UsJurisdictions.normalize("US"))
        assertNull(UsJurisdictions.normalize(""))
    }
}
