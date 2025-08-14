package com.braintreepayments.api.paypal

import org.json.JSONObject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import kotlin.test.Test

@RunWith(RobolectricTestParameterInjector::class)
class PayPalPaymentResourceUnitTest {

    @Test
    fun `fromJson parses redirectUrl from one-time PaymentResource app switch flow`() {
        val oneTimePaymentJson = JSONObject()
            .put("paymentResource", JSONObject()
                .put("redirectUrl", "www.example.com/redirect")
                .put("launchPayPalApp", true)
            ).toString()

        val sut = PayPalPaymentResource.fromJson(oneTimePaymentJson)
        assertEquals("www.example.com/redirect", sut.redirectUrl)
        assertTrue(sut.isAppSwitchFlow)
    }

    @Test
    fun `fromJson parses redirectUrl from one-time PaymentResource fallback flow`() {
        val oneTimePaymentJson = JSONObject()
            .put("paymentResource", JSONObject()
                .put("redirectUrl", "www.example.com/redirect")
                .put("launchPayPalApp", false)
            ).toString()

        val sut = PayPalPaymentResource.fromJson(oneTimePaymentJson)
        assertEquals("www.example.com/redirect", sut.redirectUrl)
        assertFalse(sut.isAppSwitchFlow)
    }

    @Test
    fun `fromJson parses redirectUrl from billingAgreement PaymentResource`() {
        val billingAgreementJson = JSONObject()
            .put("agreementSetup", JSONObject()
                .put("approvalUrl", "www.example.com/redirect")
            ).toString()

        val sut = PayPalPaymentResource.fromJson(billingAgreementJson)
        assertEquals("www.example.com/redirect", sut.redirectUrl)
        assertFalse(sut.isAppSwitchFlow)
    }

    @Test
    fun `fromJson parses redirectUrl from billingAgreement PaymentResource returns PayPalRedirectUrl`() {
        val billingAgreementJson = JSONObject()
            .put("agreementSetup", JSONObject()
                .put("approvalUrl", "www.example.com/redirect")
                .put("paypalAppApprovalUrl", "www.paypal.example.com/redirect")
            ).toString()

        val sut = PayPalPaymentResource.fromJson(billingAgreementJson)
        assertEquals("www.paypal.example.com/redirect", sut.redirectUrl)
        assertTrue(sut.isAppSwitchFlow)
    }
}
