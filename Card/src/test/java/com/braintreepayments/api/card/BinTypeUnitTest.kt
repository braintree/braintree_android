package com.braintreepayments.api.card

import org.junit.Test
import kotlin.test.assertEquals

class BinTypeUnitTest {

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(BinType.Yes, BinType.fromString("YES"))
    }

    @Test
    fun `fromString returns No`() {
        assertEquals(BinType.No, BinType.fromString("no"))
    }

    @Test
    fun `fromString returns Unknown for an unknown string`() {
        assertEquals(BinType.Unknown, BinType.fromString("otherString"))
    }
}
