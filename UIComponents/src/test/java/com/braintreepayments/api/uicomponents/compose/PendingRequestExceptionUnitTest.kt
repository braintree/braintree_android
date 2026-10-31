package com.braintreepayments.api.uicomponents.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingRequestExceptionUnitTest {

    @Test
    fun `default message describes pending request recovery failure`() {
        assertEquals(
            "Unable to recover pending request. Cannot complete flow.",
            PendingRequestException().message
        )
    }

    @Test
    fun `custom message is preserved`() {
        assertEquals("custom", PendingRequestException("custom").message)
    }
}
