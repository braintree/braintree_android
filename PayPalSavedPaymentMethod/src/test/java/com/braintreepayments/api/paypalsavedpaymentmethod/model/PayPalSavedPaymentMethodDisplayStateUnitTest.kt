package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ConfigurationException
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PayPalSavedPaymentMethodDisplayStateUnitTest {

    private val card = PayPalSavedPaymentMethod(
        type = PayPalSavedPaymentMethodType.CARD,
        label = "Visa",
        imageUrl = "https://example.com/visa.png",
        lastDigits = "1234",
        subtype = null
    )
    private val bank = card.copy(type = PayPalSavedPaymentMethodType.BANK, label = "CREDIT UNION 1")
    private val payer = PayPalPayer(email = "buyer@example.com", isEditable = true)

    @Test
    fun `summary with a funding instrument maps to Content with the first instrument`() {
        val summary = PayPalSavedPaymentMethodSummary(paymentMethods = listOf(card, bank), payer = payer)

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(card), summary.toDisplayState())
    }

    @Test
    fun `summary with only a payer maps to NoFundingInstrument with the payer email and editable flag`() {
        val summary = PayPalSavedPaymentMethodSummary(paymentMethods = emptyList(), payer = payer)

        assertEquals(
            PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true),
            summary.toDisplayState()
        )
    }

    @Test
    fun `summary with a payer that can't edit maps to a NoFundingInstrument that isn't editable`() {
        val summary = PayPalSavedPaymentMethodSummary(
            paymentMethods = emptyList(),
            payer = payer.copy(isEditable = false)
        )

        assertEquals(
            PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = false),
            summary.toDisplayState()
        )
    }

    @Test
    fun `summary with no funding instrument and no payer maps to Error`() {
        val summary = PayPalSavedPaymentMethodSummary(paymentMethods = emptyList(), payer = null)

        assertEquals(PayPalSavedPaymentMethodDisplayState.Error, summary.toDisplayState())
    }

    @Test
    fun `connection failures map to NoNetwork`() {
        val errors = listOf(
            UnknownHostException(),
            ConnectException(),
            NoRouteToHostException(),
            SocketException("Connection reset"),
            SocketTimeoutException()
        )
        errors.forEach { error ->
            assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, error.toDisplayState())
        }
    }

    @Test
    fun `a connection failure wrapped as the cause maps to NoNetwork`() {
        val error = IOException("request failed", UnknownHostException("api.braintreegateway.com"))

        assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, error.toDisplayState())
    }

    @Test
    fun `a configuration fetch that fails while offline maps to NoNetwork`() {
        val error = ConfigurationException("Request for configuration has failed", UnknownHostException())

        assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, error.toDisplayState())
    }

    @Test
    fun `server and parsing errors map to Error`() {
        val errors = listOf(BraintreeException("GraphQL error"), IOException("HTTP 500"), JSONException("bad json"))

        errors.forEach { error ->
            assertEquals(PayPalSavedPaymentMethodDisplayState.Error, error.toDisplayState())
        }
    }
}
