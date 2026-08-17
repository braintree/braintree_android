package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalBetaApi::class)
@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodSummaryUnitTest {

    @Test
    fun fromJson_parsesInstrumentResponse() {
        val json = JSONObject(
            """
            {"data":{"paypalFundingInstrumentDetails":{"payer":null,"paymentMethods":[
              {"label":"CREDIT UNION 1","imageUrl":"https://x/generic_bank.png",
               "lastDigits":"3357","type":"BANK","subtype":null}]}}}
            """.trimIndent()
        )

        val result = PayPalSavedPaymentMethodSummary.fromJson(json)

        assertNull(result.paypalPayer)
        val instrument = result.primaryInstrument!!
        assertEquals("CREDIT UNION 1", instrument.label)
        assertEquals("https://x/generic_bank.png", instrument.imageUrl)
        assertEquals("3357", instrument.lastDigits)
        assertEquals("BANK", instrument.type)
        assertNull(instrument.subtype)
    }

    @Test
    fun fromJson_parsesDisplayOnlyResponse() {
        val json = JSONObject(
            """
            {"data":{"paypalFundingInstrumentDetails":{"payer":{"email":"buyer@example.com",
              "editable":true},"paymentMethods":[]}}}
            """.trimIndent()
        )

        val result = PayPalSavedPaymentMethodSummary.fromJson(json)

        assertTrue(result.paypalSavedPaymentMethods.isEmpty())
        assertNull(result.primaryInstrument)
        assertEquals("buyer@example.com", result.paypalPayer?.email)
        assertEquals(true, result.paypalPayer?.editable)
    }

    @Test
    fun fromJson_returnsEmptyNoFi_whenDataNull() {
        val json = JSONObject("""{"data":{"paypalFundingInstrumentDetails":null}}""")

        val result = PayPalSavedPaymentMethodSummary.fromJson(json)

        assertTrue(result.paypalSavedPaymentMethods.isEmpty())
        assertNull(result.paypalPayer)
        assertNull(result.primaryInstrument)
    }

    @Test
    fun exception_fromGraphQLResponse_readsErrorClassAndMessage() {
        val json = JSONObject(
            """
            {"errors":[{"message":"PayPal access token not found for merchant account.",
              "extensions":{"errorClass":"AUTHENTICATION","errorType":"developer_error"}}],
             "data":{"paypalFundingInstrumentDetails":null}}
            """.trimIndent()
        )

        val exception = PayPalSavedPaymentMethodSummaryException.fromGraphQLResponse(json)

        assertEquals("AUTHENTICATION", exception.errorClass)
        assertEquals("PayPal access token not found for merchant account.", exception.message)
    }

    @Test
    fun exception_fromGraphQLResponse_readsInternalErrorClass() {
        val json = JSONObject(
            """
            {"errors":[{"message":"An internal error occurred.",
              "extensions":{"errorClass":"INTERNAL","errorType":"unknown_error"}}],
             "data":{"paypalFundingInstrumentDetails":null}}
            """.trimIndent()
        )

        val exception = PayPalSavedPaymentMethodSummaryException.fromGraphQLResponse(json)

        assertEquals("INTERNAL", exception.errorClass)
    }
}
