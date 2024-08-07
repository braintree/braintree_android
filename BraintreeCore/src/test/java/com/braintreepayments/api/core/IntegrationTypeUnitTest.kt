package com.braintreepayments.api.core

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IntegrationTypeUnitTest {

    @Test
    fun `fromString returns null when string value is not found`() {
        assertNull(IntegrationType.fromString("unknownValue"))
    }

    @Test
    fun `fromString returns null when null is passed in`() {
        assertNull(IntegrationType.fromString(null))
    }

    @Test
    fun `fromString returns CUSTOM`() {
        assertEquals(IntegrationType.CUSTOM, IntegrationType.fromString("custom"))
    }

    @Test
    fun `fromString returns DROP_IN`() {
        assertEquals(IntegrationType.DROP_IN, IntegrationType.fromString("dropin"))
    }
}