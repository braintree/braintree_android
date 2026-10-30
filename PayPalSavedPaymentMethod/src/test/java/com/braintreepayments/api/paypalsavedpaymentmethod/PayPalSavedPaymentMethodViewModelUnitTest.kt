@file:OptIn(ExperimentalBetaApi::class, ExperimentalCoroutinesApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessageItem
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalPayer
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodSummary
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class PayPalSavedPaymentMethodViewModelUnitTest {

    private val dispatcher = StandardTestDispatcher()
    private val client = mockk<PayPalSavedPaymentMethodClient>()
    private val launcher = mockk<PayPalLauncher>(relaxed = true)
    private val activity = mockk<ComponentActivity>()
    private val appLinkReturnUrl = uri(scheme = "https", host = "merchant.example.com")
    private val savedState = SavedStateHandle()
    private lateinit var sut: PayPalSavedPaymentMethodViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } returns summary(CARD)
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } returns MESSAGE_RESULT
        sut = newViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Loading

    @Test
    fun `the first request loads the default funding instrument and the Pay Later message`() = runTest(dispatcher) {
        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(CARD), sut.state.value.fundingInstrument)
        assertEquals(MESSAGE, sut.state.value.creditMessaging)
        coVerify { client.fetchPaymentMethod(BUYER_DEFAULT, null, "ma-1") }
        coVerify { client.fetchCreditPresentmentMessages("55.00", "USD") }
    }

    @Test
    fun `a failed load shows the offline state for a connection failure, an error otherwise`() = runTest(dispatcher) {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws UnknownHostException()
        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()
        assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, sut.state.value.fundingInstrument)

        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws BraintreeException("boom")
        sut = newViewModel()
        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()
        assertEquals(PayPalSavedPaymentMethodDisplayState.Error, sut.state.value.fundingInstrument)
    }

    @Test
    fun `a failed Pay Later load only hides its row`() = runTest(dispatcher) {
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } throws IllegalStateException("boom")

        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
    }

    @Test
    fun `a cancelled load isn't shown as an error`() = runTest(dispatcher) {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws CancellationException()

        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Loading, sut.state.value.fundingInstrument)
    }

    @Test
    fun `a new request loads only the message again, the same request nothing`() = runTest(dispatcher) {
        sut.load(request("10.00"), showCreditMessaging = true)
        advanceUntilIdle()
        sut.onRequestChanged(request("10.00"), showCreditMessaging = true)
        sut.onRequestChanged(request("20.00"), showCreditMessaging = true)
        advanceUntilIdle()

        coVerify(exactly = 2) { client.fetchCreditPresentmentMessages(any(), any()) }
        coVerify(exactly = 1) { client.fetchPaymentMethod(any(), any(), any()) }
    }

    @Test
    fun `turning the message off hides it, and turning it back on loads it`() = runTest(dispatcher) {
        sut.load(request("10.00"), showCreditMessaging = false)
        advanceUntilIdle()
        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        coVerify(exactly = 0) { client.fetchCreditPresentmentMessages(any(), any()) }

        sut.onRequestChanged(request("10.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(MESSAGE, sut.state.value.creditMessaging)
    }

    // Starting an edit

    @Test
    fun `startEdit shows the edit in progress and returns the pending request`() = runTest(dispatcher) {
        val authRequest = mockk<PayPalPaymentAuthRequest.ReadyToLaunch>()
        val pending = PayPalPendingRequest.Started("pending")
        val created = CompletableDeferred<PayPalSavedPaymentMethodAuthRequest>()
        coEvery { client.createEditFundingInstrumentAuthRequest(activity, CHECKOUT) } coAnswers { created.await() }
        every { launcher.launch(activity, authRequest) } returns pending

        val result = async { sut.startEdit(activity, launcher, CHECKOUT) }
        runCurrent()
        assertTrue(sut.state.value.isEditing)
        created.complete(PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch(authRequest))

        assertSame(pending, result.await())
        assertFalse(sut.state.value.isEditing)
    }

    @Test
    fun `startEdit returns a failure when the auth request fails or throws`() = runTest(dispatcher) {
        val error = BraintreeException("auth failed")
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } returns
            PayPalSavedPaymentMethodAuthRequest.Failure(error)
        assertSame(error, (sut.startEdit(activity, launcher, CHECKOUT) as PayPalPendingRequest.Failure).error)

        val thrown = IOException("network")
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } throws thrown
        assertSame(thrown, (sut.startEdit(activity, launcher, CHECKOUT) as PayPalPendingRequest.Failure).error)
    }

    @Test
    fun `startEdit fails without a request when not hosted in a ComponentActivity`() = runTest(dispatcher) {
        val result = sut.startEdit(mockk<Context>(), launcher, CHECKOUT)

        assertEquals(
            PayPalSavedPaymentMethodViewModel.NO_COMPONENT_ACTIVITY_MESSAGE,
            (result as PayPalPendingRequest.Failure).error.message
        )
        coVerify(exactly = 0) { client.createEditFundingInstrumentAuthRequest(any(), any()) }
    }

    @Test
    fun `a second edit step while one is running is ignored`() = runTest(dispatcher) {
        val created = CompletableDeferred<PayPalSavedPaymentMethodAuthRequest>()
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } coAnswers { created.await() }

        val first = async { sut.startEdit(activity, launcher, CHECKOUT) }
        runCurrent()

        assertNull(sut.startEdit(activity, launcher, CHECKOUT))
        sut.handleReturnToApp("pending", launcher, intentWith(null))
        created.complete(PayPalSavedPaymentMethodAuthRequest.Failure(BraintreeException("failed")))
        first.await()
        coVerify(exactly = 1) { client.createEditFundingInstrumentAuthRequest(any(), any()) }
        verify(exactly = 0) { launcher.handleReturnToApp(any(), any()) }
    }

    @Test
    fun `a cancelled edit step doesn't leave the edit in progress`() = runTest(dispatcher) {
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } coAnswers {
            CompletableDeferred<Nothing>().await()
        }

        val job = launch { sut.startEdit(activity, launcher, CHECKOUT) }
        runCurrent()
        job.cancel()
        runCurrent()

        assertFalse(sut.state.value.isEditing)
    }

    // Completing an edit

    @Test
    fun `a successful edit is delivered, saved, and loads the funding instrument the buyer picked`() =
        runTest(dispatcher) {
            sut.load(request("55.00"), showCreditMessaging = true)
            advanceUntilIdle()
            val success = givenReturn(tokenized = success("order-1"))
            coEvery { client.fetchPaymentMethod(any(), "order-1", any()) } returns summary(EDITED)
            val intent = intentWith(appLinkReturnUrl)

            sut.handleReturnToApp("pending", launcher, intent)
            advanceUntilIdle()

            assertSame(success, sut.results.first())
            assertTrue(sut.state.value.isEditCompleted)
            assertEquals("order-1", sut.state.value.editedOrderId)
            assertEquals(true, savedState.get<Boolean>("is_edit_completed"))
            assertEquals("order-1", savedState.get<String>("edited_order_id"))
            assertEquals(PayPalSavedPaymentMethodDisplayState.Content(EDITED), sut.state.value.fundingInstrument)
            coVerify {
                client.fetchPaymentMethod(BUYER_UPDATED, "order-1", "ma-1")
            }
            verify { launcher.restorePendingRequest("pending") }
            verify { intent.data = null }
        }

    @Test
    fun `after an edit the Pay Later message is hidden and isn't loaded again`() = runTest(dispatcher) {
        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()
        givenReturn(tokenized = success("order-1"))

        sut.handleReturnToApp("pending", launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()
        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        sut.onRequestChanged(request("99.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        coVerify(exactly = 1) { client.fetchCreditPresentmentMessages(any(), any()) }
    }

    @Test
    fun `only the brand shows when the edited funding instrument can't be loaded`() = runTest(dispatcher) {
        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()

        givenReturn(tokenized = success(null))
        sut.handleReturnToApp("pending", launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()
        assertEquals(PayPalSavedPaymentMethodDisplayState.Error, sut.state.value.fundingInstrument)
        coVerify(exactly = 0) { client.fetchPaymentMethod(BUYER_UPDATED, any(), any()) }
    }

    @Test
    fun `the edited funding instrument maps a failed load like the first load`() = runTest(dispatcher) {
        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()
        coEvery { client.fetchPaymentMethod(any(), "order-1", any()) } throws UnknownHostException()
        givenReturn(tokenized = success("order-1"))

        sut.handleReturnToApp("pending", launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, sut.state.value.fundingInstrument)
    }

    @Test
    fun `an edit that completes while the first load is running wins over that load`() = runTest(dispatcher) {
        val firstLoad = CompletableDeferred<PayPalSavedPaymentMethodSummary>()
        coEvery { client.fetchPaymentMethod(any(), null, any()) } coAnswers { firstLoad.await() }
        coEvery { client.fetchPaymentMethod(any(), "order-1", any()) } returns summary(EDITED)
        givenReturn(tokenized = success("order-1"))
        sut.load(request("55.00"), showCreditMessaging = true)
        runCurrent()

        sut.handleReturnToApp("pending", launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()
        firstLoad.complete(summary(CARD))
        advanceUntilIdle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(EDITED), sut.state.value.fundingInstrument)
    }

    @Test
    fun `a failed return is delivered and its return URL cleared, a cancel keeps it`() = runTest(dispatcher) {
        val failed = intentWith(uri(scheme = DEEP_LINK_SCHEME, host = "x"))
        val error = BraintreeException("return failed")
        every { launcher.handleReturnToApp(any(), failed) } returns authFailure(error)
        sut.handleReturnToApp("pending", launcher, failed)
        advanceUntilIdle()
        assertSame(error, (sut.results.first() as PayPalResult.Failure).error)
        verify { failed.data = null }

        val cancelled = intentWith(appLinkReturnUrl)
        every { launcher.handleReturnToApp(any(), cancelled) } returns PayPalPaymentAuthResult.NoResult
        sut.handleReturnToApp("another-pending", launcher, cancelled)
        advanceUntilIdle()
        assertSame(PayPalResult.Cancel, sut.results.first())
        verify(exactly = 0) { cancelled.data = null }
        assertFalse(sut.state.value.isEditCompleted)
    }

    @Test
    fun `a return that can't be restored fails and its return URL is cleared`() = runTest(dispatcher) {
        val error = IllegalStateException("bad pending request")
        every { launcher.restorePendingRequest(any()) } throws error
        val intent = intentWith(uri(scheme = "HTTPS", host = "Merchant.Example.com"))

        sut.handleReturnToApp("pending", launcher, intent)
        advanceUntilIdle()

        assertSame(error, (sut.results.first() as PayPalResult.Failure).error)
        verify { intent.data = null }
    }

    @Test
    fun `a return without an intent fails, and the merchant's own deep links are left alone`() = runTest(dispatcher) {
        sut.handleReturnToApp("pending", launcher, null)
        advanceUntilIdle()
        assertEquals(
            PayPalSavedPaymentMethodViewModel.NO_INTENT_MESSAGE,
            (sut.results.first() as PayPalResult.Failure).error.message
        )

        val merchantLink = intentWith(uri(scheme = "https", host = "other.example.com"))
        every { launcher.handleReturnToApp(any(), merchantLink) } returns authFailure(BraintreeException("failed"))
        sut.handleReturnToApp("another-pending", launcher, merchantLink)
        advanceUntilIdle()
        sut.results.first()
        verify(exactly = 0) { merchantLink.data = null }
    }

    @Test
    fun `a completed return isn't completed again, even after the process is recreated`() = runTest(dispatcher) {
        givenReturn(tokenized = success("order-1"))
        sut.handleReturnToApp("pending", launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()
        sut.results.first()

        sut.handleReturnToApp("pending", launcher, intentWith(null))
        newViewModel().handleReturnToApp("pending", launcher, intentWith(null))
        advanceUntilIdle()

        verify(exactly = 1) { launcher.handleReturnToApp(any(), any()) }
    }

    @Test
    fun `a return whose result wasn't delivered before the process died is completed again`() = runTest(dispatcher) {
        givenReturn(tokenized = success("order-1"))
        sut.handleReturnToApp("pending", launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()
        sut.handleReturnToApp("pending", launcher, intentWith(null))
        advanceUntilIdle()
        verify(exactly = 1) { launcher.handleReturnToApp(any(), any()) }

        newViewModel().handleReturnToApp("pending", launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()

        verify(exactly = 2) { launcher.handleReturnToApp(any(), any()) }
    }

    @Test
    fun `a cancelled return can still be completed`() = runTest(dispatcher) {
        every { launcher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult
        sut.handleReturnToApp("pending", launcher, intentWith(null))
        advanceUntilIdle()
        assertSame(PayPalResult.Cancel, sut.results.first())

        val success = givenReturn(tokenized = success("order-1"))
        sut.handleReturnToApp("pending", launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()

        assertSame(success, sut.results.first())
    }

    @Test
    fun `only one view at a time can show the view model`() {
        val first = "view-1"
        val second = "view-2"

        assertTrue(sut.attachView(first))
        assertTrue(sut.isAttachedToAnotherView(second))
        assertFalse(sut.attachView(second))
        sut.detachView(second)
        assertTrue(sut.isAttachedToAnotherView(second))

        sut.detachView(first)
        assertTrue(sut.attachView(second))
    }

    // Saved state and configuration

    @Test
    fun `an edit saved before the process died loads the funding instrument the buyer picked`() = runTest(dispatcher) {
        savedState["is_edit_completed"] = true
        savedState["edited_order_id"] = "order-1"
        coEvery { client.fetchPaymentMethod(any(), "order-1", any()) } returns summary(EDITED)
        sut = newViewModel()

        sut.load(request("55.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(EDITED), sut.state.value.fundingInstrument)
        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        coVerify(exactly = 0) { client.fetchPaymentMethod(any(), null, any()) }
        coVerify(exactly = 0) { client.fetchCreditPresentmentMessages(any(), any()) }
    }

    private fun newViewModel() =
        PayPalSavedPaymentMethodViewModel(savedState, "client-token", appLinkReturnUrl, DEEP_LINK_SCHEME) { _, _, _ ->
            client
        }

    private fun givenReturn(tokenized: PayPalResult): PayPalResult {
        val authResult = mockk<PayPalPaymentAuthResult.Success>()
        every { launcher.handleReturnToApp(any(), any()) } returns authResult
        coEvery { client.tokenize(authResult) } returns tokenized
        return tokenized
    }

    private fun success(paymentId: String?): PayPalResult.Success {
        val nonce = mockk<PayPalAccountNonce> { every { this@mockk.paymentId } returns paymentId }
        return PayPalResult.Success(nonce)
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

    private fun request(amount: String, merchantAccountId: String = "ma-1") =
        PayPalSavedPaymentMethodRequest(amount, "USD", merchantAccountId)

    private fun summary(paymentMethod: PayPalSavedPaymentMethod) =
        PayPalSavedPaymentMethodSummary(listOf(paymentMethod), PayPalPayer("buyer@example.com", isEditable = true))

    private companion object {
        const val DEEP_LINK_SCHEME = "com.merchant.braintree"
        val BUYER_DEFAULT = PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT
        val BUYER_UPDATED = PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT
        val CHECKOUT = PayPalCheckoutRequest(amount = "55.00", hasUserLocationConsent = true)
        val CARD = PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.CARD, "Visa", null, "1234", null)
        val EDITED = PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.BANK, "Chase", null, "9876", null)
        val MESSAGE_RESULT = PayPalCreditMessagingResult(
            mainItems = listOf(PayPalCreditMessageItem(null, "Pay in 4", null, null, null, null, null)),
            disclaimerItems = emptyList(),
            actionItems = emptyList(),
            messageId = null,
            messageType = null,
            impressionUrl = null
        )
        val MESSAGE =
            PayPalCreditMessagingDisplayState.Content(PayPalCreditMessagingContent("Pay in 4", learnMore = null))
    }
}
