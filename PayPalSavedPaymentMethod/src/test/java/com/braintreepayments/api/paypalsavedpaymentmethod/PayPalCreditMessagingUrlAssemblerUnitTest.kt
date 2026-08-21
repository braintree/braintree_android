package com.braintreepayments.api.paypalsavedpaymentmethod

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalCreditMessagingUrlAssemblerUnitTest {

    @Test
    fun assembleURL_whenProduction_returnsProductionUrl() {
        assertEquals(
            "https://api.paypal.com/v2/credit/fetch-presentment-messages",
            PayPalCreditMessagingUrlAssembler.assembleURL("production")
        )
    }

    @Test
    fun assembleURL_whenSandbox_returnsSandboxUrl() {
        assertEquals(
            "https://api.sandbox.paypal.com/v2/credit/fetch-presentment-messages",
            PayPalCreditMessagingUrlAssembler.assembleURL("sandbox")
        )
    }

    @Test
    fun assembleURL_whenUnrecognizedOrNull_defaultsToSandboxUrl() {
        assertEquals(
            "https://api.sandbox.paypal.com/v2/credit/fetch-presentment-messages",
            PayPalCreditMessagingUrlAssembler.assembleURL("development")
        )
        assertEquals(
            "https://api.sandbox.paypal.com/v2/credit/fetch-presentment-messages",
            PayPalCreditMessagingUrlAssembler.assembleURL(null)
        )
    }
}