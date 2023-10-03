package com.braintreepayments.api

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodNonceUnitTest {
    @Test
    fun constructor() {
        val nonce = PaymentMethodNonce("fake-nonce", true)
        Assert.assertEquals("fake-nonce", nonce.string)
        Assert.assertTrue(nonce.isDefault)
    }
}
