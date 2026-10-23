@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessageItem
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalPayer
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodSummary
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import kotlin.test.assertFailsWith

class PayPalSavedPaymentMethodControllerUnitTest {

    private val client = mockk<PayPalSavedPaymentMethodClient>()
    private val payPalLauncher = mockk<PayPalLauncher>(relaxed = true)
    private val appLinkReturnUrl = uri(scheme = "https", host = "merchant.example.com")
    private val sut = PayPalSavedPaymentMethodController(client, appLinkReturnUrl, DEEP_LINK_SCHEME)

    @Test
    fun `loadFundingInstrument fetches the default billing agreement and maps it to content`() = runTest {
        coEvery {
            client.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT, null, "ma-1")
        } returns PayPalSavedPaymentMethodSummary(listOf(CARD), payer = null)

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(CARD), sut.loadFundingInstrument("ma-1"))
    }

    @Test
    fun `loadFundingInstrument maps a connection failure to NoNetwork`() = runTest {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws UnknownHostException()

        assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, sut.loadFundingInstrument(null))
    }

    @Test
    fun `loadFundingInstrument re-throws a cancellation instead of mapping it`() = runTest {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws CancellationException()

        assertFailsWith<CancellationException> { sut.loadFundingInstrument(null) }
    }

    @Test
    fun `loadEditedFundingInstrument fetches the updated billing agreement for the order`() = runTest {
        coEvery {
            client.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT, "order-1", null)
        } returns PayPalSavedPaymentMethodSummary(listOf(CARD), payer = null)

        assertEquals(
            PayPalSavedPaymentMethodDisplayState.Content(CARD),
            sut.loadEditedFundingInstrument("order-1", null)
        )
    }

    @Test
    fun `loadEditedFundingInstrument keeps an email-only result`() = runTest {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } returns
            PayPalSavedPaymentMethodSummary(emptyList(), PayPalPayer("buyer@example.com", isEditable = true))

        assertEquals(
            PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true),
            sut.loadEditedFundingInstrument("order-1", null)
        )
    }

    @Test
    fun `loadEditedFundingInstrument hides the component without fetching when there is no order`() = runTest {
        assertEquals(PayPalSavedPaymentMethodDisplayState.Hidden, sut.loadEditedFundingInstrument(null, null))
        coVerify(exactly = 0) { client.fetchPaymentMethod(any(), any(), any()) }
    }

    @Test
    fun `loadEditedFundingInstrument hides the component when the fetch fails or returns nothing`() = runTest {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws BraintreeException("boom")
        assertEquals(PayPalSavedPaymentMethodDisplayState.Hidden, sut.loadEditedFundingInstrument("order-1", null))

        coEvery { client.fetchPaymentMethod(any(), any(), any()) } returns
            PayPalSavedPaymentMethodSummary(emptyList(), payer = null)
        assertEquals(PayPalSavedPaymentMethodDisplayState.Hidden, sut.loadEditedFundingInstrument("order-1", null))
    }

    @Test
    fun `loadCreditMessaging fetches the message for the request amount and currency`() = runTest {
        coEvery { client.fetchCreditPresentmentMessages("55.00", "USD") } returns PayPalCreditMessagingResult(
            mainItems = listOf(PayPalCreditMessageItem(null, "Pay in 4", null, null, null, null, null)),
            disclaimerItems = emptyList(),
            actionItems = emptyList(),
            messageId = null,
            messageType = null,
            impressionUrl = null
        )

        assertEquals(
            PayPalCreditMessagingDisplayState.Content(PayPalCreditMessagingContent("Pay in 4", null, null)),
            sut.loadCreditMessaging(PayPalSavedPaymentMethodRequest("55.00", "USD"))
        )
    }

    @Test
    fun `loadCreditMessaging hides the row when the fetch fails`() = runTest {
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } throws BraintreeException("boom")

        assertEquals(
            PayPalCreditMessagingDisplayState.Hidden,
            sut.loadCreditMessaging(PayPalSavedPaymentMethodRequest("55.00", "USD"))
        )
    }

    @Test
    fun `startEditFlow launches the paysheet and returns the pending request`() = runTest {
        val activity = mockk<ComponentActivity>()
        val authRequest = mockk<PayPalPaymentAuthRequest.ReadyToLaunch>()
        val pending = PayPalPendingRequest.Started("pending")
        coEvery { client.createEditFundingInstrumentAuthRequest(activity, CHECKOUT_REQUEST) } returns
            PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch(authRequest)
        every { payPalLauncher.launch(activity, authRequest) } returns pending

        assertSame(pending, sut.startEditFlow(activity, payPalLauncher, CHECKOUT_REQUEST))
    }

    @Test
    fun `startEditFlow returns a failure when the auth request fails or throws`() = runTest {
        val activity = mockk<ComponentActivity>()
        val error = BraintreeException("auth failed")
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } returns
            PayPalSavedPaymentMethodAuthRequest.Failure(error)
        val failed = sut.startEditFlow(activity, payPalLauncher, CHECKOUT_REQUEST)
        assertSame(error, (failed as PayPalPendingRequest.Failure).error)

        val thrown = IOException("network")
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } throws thrown
        val threw = sut.startEditFlow(activity, payPalLauncher, CHECKOUT_REQUEST)
        assertSame(thrown, (threw as PayPalPendingRequest.Failure).error)
    }

    @Test
    fun `startEditFlow fails without a request when not hosted in a ComponentActivity`() = runTest {
        val result = sut.startEditFlow(mockk<Context>(), payPalLauncher, CHECKOUT_REQUEST)

        assertEquals(
            PayPalSavedPaymentMethodController.NO_COMPONENT_ACTIVITY_MESSAGE,
            (result as PayPalPendingRequest.Failure).error.message
        )
        coVerify(exactly = 0) { client.createEditFundingInstrumentAuthRequest(any(), any()) }
    }

    @Test
    fun `completeEditFlow tokenizes a successful return and clears the return url`() = runTest {
        val intent = intentWith(uri(scheme = "https", host = "merchant.example.com"))
        val authResult = mockk<PayPalPaymentAuthResult.Success>()
        val tokenized = mockk<PayPalResult.Success>()
        every { payPalLauncher.handleReturnToApp(any(), intent) } returns authResult
        coEvery { client.tokenize(authResult) } returns tokenized

        assertSame(tokenized, sut.completeEditFlow("pending", payPalLauncher, intent))
        verify { payPalLauncher.restorePendingRequest("pending") }
        verify { intent.data = null }
    }

    @Test
    fun `completeEditFlow reports a failed return and clears a deep link return url`() = runTest {
        val intent = intentWith(uri(scheme = DEEP_LINK_SCHEME, host = "x"))
        val error = BraintreeException("return failed")
        every { payPalLauncher.handleReturnToApp(any(), intent) } returns authFailure(error)

        val result = sut.completeEditFlow("pending", payPalLauncher, intent)

        assertSame(error, (result as PayPalResult.Failure).error)
        verify { intent.data = null }
    }

    @Test
    fun `completeEditFlow reports no result as a cancel and keeps the intent data`() = runTest {
        val intent = intentWith(uri(scheme = "https", host = "merchant.example.com"))
        every { payPalLauncher.handleReturnToApp(any(), intent) } returns PayPalPaymentAuthResult.NoResult

        assertSame(PayPalResult.Cancel, sut.completeEditFlow("pending", payPalLauncher, intent))
        verify(exactly = 0) { intent.data = null }
    }

    @Test
    fun `completeEditFlow matches the App Link return url regardless of case`() = runTest {
        val intent = intentWith(uri(scheme = "HTTPS", host = "Merchant.Example.com"))
        every { payPalLauncher.handleReturnToApp(any(), intent) } returns authFailure(BraintreeException("failed"))

        sut.completeEditFlow("pending", payPalLauncher, intent)

        verify { intent.data = null }
    }

    @Test
    fun `completeEditFlow leaves the merchant's own deep links alone`() = runTest {
        val intent = intentWith(uri(scheme = "https", host = "other.example.com"))
        every { payPalLauncher.handleReturnToApp(any(), intent) } returns authFailure(BraintreeException("failed"))

        sut.completeEditFlow("pending", payPalLauncher, intent)

        verify(exactly = 0) { intent.data = null }
    }

    @Test
    fun `completeEditFlow fails when there is no intent or the pending request can't be restored`() = runTest {
        val noIntent = sut.completeEditFlow("pending", payPalLauncher, null)
        assertEquals(
            PayPalSavedPaymentMethodController.NO_INTENT_MESSAGE,
            (noIntent as PayPalResult.Failure).error.message
        )

        val error = IllegalStateException("bad pending request")
        every { payPalLauncher.restorePendingRequest(any()) } throws error
        val result = sut.completeEditFlow("pending", payPalLauncher, intentWith(null))
        assertSame(error, (result as PayPalResult.Failure).error)
    }

    @Test
    fun `every load and the edit flow re-throw a cancellation instead of reporting it`() = runTest {
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } throws CancellationException()
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } throws CancellationException()
        coEvery { client.tokenize(any()) } throws CancellationException()
        every { payPalLauncher.handleReturnToApp(any(), any()) } returns mockk<PayPalPaymentAuthResult.Success>()

        assertFailsWith<CancellationException> {
            sut.loadCreditMessaging(PayPalSavedPaymentMethodRequest("55.00", "USD"))
        }
        assertFailsWith<CancellationException> {
            sut.startEditFlow(mockk<ComponentActivity>(), payPalLauncher, CHECKOUT_REQUEST)
        }
        assertFailsWith<CancellationException> { sut.completeEditFlow("pending", payPalLauncher, intentWith(null)) }
    }

    private fun authFailure(error: Exception): PayPalPaymentAuthResult.Failure = mockk {
        every { this@mockk.error } returns error
    }

    private fun uri(scheme: String, host: String): Uri = mockk {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
    }

    private fun intentWith(data: Uri?): Intent = mockk(relaxed = true) {
        every { this@mockk.data } returns data
    }

    private companion object {
        const val DEEP_LINK_SCHEME = "com.merchant.braintree"
        val CARD = PayPalSavedPaymentMethod(
            type = PayPalSavedPaymentMethodType.CARD,
            label = "Visa",
            imageUrl = null,
            lastDigits = "1234",
            subtype = null
        )
        val CHECKOUT_REQUEST = PayPalCheckoutRequest(amount = "55.00", hasUserLocationConsent = true)
    }
}
