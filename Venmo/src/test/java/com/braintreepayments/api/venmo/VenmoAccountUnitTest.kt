package com.braintreepayments.api.venmo

import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.PaymentMethod
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class VenmoAccountUnitTest {

    @Test
    fun `correctly builds Venmo vault Request`() {
        val sut = VenmoAccount("some-nonce", null, PaymentMethod.DEFAULT_SOURCE, IntegrationType.CUSTOM)
        val fullJson = sut.buildJSON()
        val venmoAccountJson = fullJson.getJSONObject("venmoAccount")
        assertEquals("some-nonce", venmoAccountJson.getString("nonce"))
    }
}
