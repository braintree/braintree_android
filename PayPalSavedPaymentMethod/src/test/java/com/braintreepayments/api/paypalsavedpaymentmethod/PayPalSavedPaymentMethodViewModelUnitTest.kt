@file:OptIn(ExperimentalBetaApi::class, ExperimentalCoroutinesApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
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
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalPayer
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodSummary
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
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
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
        coEvery { client.fetchPaymentMethod(any(), ORDER_ID, any()) } returns summary(EDITED)
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } returns MESSAGE_RESULT
        every { activity.isFinishing } returns false
        every { activity.isDestroyed } returns false
        sut = newViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Loading

    @Test
    fun `the first request loads the default funding instrument and the Pay Later message`() = runTest(dispatcher) {
        loadAndSettle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(CARD), sut.state.value.fundingInstrument)
        assertEquals(MESSAGE, sut.state.value.creditMessaging)
        coVerify { client.fetchPaymentMethod(BUYER_DEFAULT, null, MERCHANT_ACCOUNT_ID) }
        coVerify { client.fetchCreditPresentmentMessages(AMOUNT, "USD") }
    }

    @Test
    fun `a load that fails to connect shows the offline state`() = runTest(dispatcher) {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws UnknownHostException()

        loadAndSettle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, sut.state.value.fundingInstrument)
    }

    @Test
    fun `a load that fails for another reason shows the error state`() = runTest(dispatcher) {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws BraintreeException("boom")

        loadAndSettle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Error, sut.state.value.fundingInstrument)
    }

    @Test
    fun `a failed load is retried when a view attaches again`() = runTest(dispatcher) {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws UnknownHostException()
        loadAndSettle()
        assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, sut.state.value.fundingInstrument)

        coEvery { client.fetchPaymentMethod(any(), any(), any()) } returns summary(CARD)
        loadAndSettle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(CARD), sut.state.value.fundingInstrument)
    }

    @Test
    fun `a failed refresh keeps the funding instrument and message shown`() = runTest(dispatcher) {
        loadAndSettle()

        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws UnknownHostException()
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } throws UnknownHostException()
        loadAndSettle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(CARD), sut.state.value.fundingInstrument)
        assertEquals(MESSAGE, sut.state.value.creditMessaging)
        coVerify(exactly = 2) { client.fetchPaymentMethod(BUYER_DEFAULT, null, MERCHANT_ACCOUNT_ID) }
        coVerify(exactly = 2) { client.fetchCreditPresentmentMessages(AMOUNT, "USD") }
    }

    @Test
    fun `a failed first Pay Later load hides its row`() = runTest(dispatcher) {
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } throws IllegalStateException("boom")

        loadAndSettle(amount = "10.00")

        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
    }

    @Test
    fun `a failed Pay Later load for a new amount hides the message quoted for the old one`() = runTest(dispatcher) {
        coEvery { client.fetchCreditPresentmentMessages("20.00", any()) } throws IllegalStateException("boom")
        loadAndSettle(amount = "10.00")

        sut.onRequestChanged(request("20.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
    }

    @Test
    fun `attaching again after an edit doesn't load the funding instrument from before it`() = runTest(dispatcher) {
        loadAndSettle()
        completeReturn()

        loadAndSettle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(EDITED), sut.state.value.fundingInstrument)
        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        coVerify(exactly = 1) { client.fetchPaymentMethod(BUYER_DEFAULT, any(), any()) }
        coVerify(exactly = 1) { client.fetchPaymentMethod(BUYER_UPDATED, any(), any()) }
    }

    @Test
    fun `a cancelled load isn't shown as an error`() = runTest(dispatcher) {
        coEvery { client.fetchPaymentMethod(any(), any(), any()) } throws CancellationException()

        loadAndSettle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Loading, sut.state.value.fundingInstrument)
    }

    @Test
    fun `a new request loads only the Pay Later message again`() = runTest(dispatcher) {
        loadAndSettle(amount = "10.00")

        sut.onRequestChanged(request("20.00"), showCreditMessaging = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { client.fetchCreditPresentmentMessages("20.00", any()) }
        coVerify(exactly = 1) { client.fetchPaymentMethod(any(), any(), any()) }
    }

    @Test
    fun `the same request loads nothing again`() = runTest(dispatcher) {
        loadAndSettle(amount = "10.00")

        sut.onRequestChanged(request("10.00"), showCreditMessaging = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { client.fetchCreditPresentmentMessages(any(), any()) }
        coVerify(exactly = 1) { client.fetchPaymentMethod(any(), any(), any()) }
    }

    @Test
    fun `an older amount's message can't land after the current one`() = runTest(dispatcher) {
        val olderResponse = CompletableDeferred<PayPalCreditMessagingResult>()
        coEvery { client.fetchCreditPresentmentMessages("10.00", any()) } coAnswers { olderResponse.await() }
        coEvery { client.fetchCreditPresentmentMessages("20.00", any()) } returns messageResult("Pay in 4 of $5")
        sut.load(request("10.00"), showCreditMessaging = true)
        runCurrent()

        sut.onRequestChanged(request("20.00"), showCreditMessaging = true)
        advanceUntilIdle()
        olderResponse.complete(messageResult("Pay in 4 of $2.50"))
        advanceUntilIdle()

        assertEquals(message("Pay in 4 of $5"), sut.state.value.creditMessaging)
    }

    @Test
    fun `a Pay Later message turned off is hidden and not loaded`() = runTest(dispatcher) {
        loadAndSettle(amount = "10.00", showCreditMessaging = false)

        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        coVerify(exactly = 0) { client.fetchCreditPresentmentMessages(any(), any()) }
    }

    @Test
    fun `turning the Pay Later message back on loads it`() = runTest(dispatcher) {
        loadAndSettle(amount = "10.00", showCreditMessaging = false)

        sut.onRequestChanged(request("10.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(MESSAGE, sut.state.value.creditMessaging)
    }

    // Starting an edit

    @Test
    fun `startEdit shows the edit in progress and returns the pending request`() = runTest(dispatcher) {
        val authRequest = mockk<PayPalPaymentAuthRequest.ReadyToLaunch>()
        val pending = PayPalPendingRequest.Started(PENDING)
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
    fun `startEdit returns the failure of an auth request that fails`() = runTest(dispatcher) {
        val error = BraintreeException("auth failed")
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } returns
            PayPalSavedPaymentMethodAuthRequest.Failure(error)

        val pending = sut.startEdit(activity, launcher, CHECKOUT)

        assertSame(error, (pending as PayPalPendingRequest.Failure).error)
    }

    @Test
    fun `startEdit returns the error of an auth request that throws`() = runTest(dispatcher) {
        val thrown = IOException("network")
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } throws thrown

        val pending = sut.startEdit(activity, launcher, CHECKOUT)

        assertSame(thrown, (pending as PayPalPendingRequest.Failure).error)
    }

    @Test
    fun `startEdit doesn't launch when the activity closed while the auth request was created`() =
        runTest(dispatcher) {
            coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } returns
                PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch(mockk())

            every { activity.isFinishing } returns true
            assertNull(sut.startEdit(activity, launcher, CHECKOUT))

            every { activity.isFinishing } returns false
            every { activity.isDestroyed } returns true
            assertNull(sut.startEdit(activity, launcher, CHECKOUT))

            verify(exactly = 0) { launcher.launch(any(), any()) }
            assertFalse(sut.state.value.isEditing)
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
    fun `startEdit while an edit step is running is ignored`() = runTest(dispatcher) {
        val created = startEditInProgress()

        assertNull(sut.startEdit(activity, launcher, CHECKOUT))
        created.complete(PayPalSavedPaymentMethodAuthRequest.Failure(BraintreeException("failed")))
        advanceUntilIdle()

        coVerify(exactly = 1) { client.createEditFundingInstrumentAuthRequest(any(), any()) }
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
            loadAndSettle()
            val success = givenReturn(success(ORDER_ID))

            val handling = sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
            advanceUntilIdle()

            assertEquals(PayPalReturnHandling.COMPLETED, handling)
            assertSame(success, sut.results.first())
            assertTrue(sut.state.value.isEditCompleted)
            assertEquals(true, savedState.get<Boolean>("is_edit_completed"))
            assertEquals(ORDER_ID, savedState.get<String>("edited_order_id"))
            assertEquals(PayPalSavedPaymentMethodDisplayState.Content(EDITED), sut.state.value.fundingInstrument)
            coVerify { client.fetchPaymentMethod(BUYER_UPDATED, ORDER_ID, MERCHANT_ACCOUNT_ID) }
            verify { launcher.restorePendingRequest(PENDING) }
        }

    @Test
    fun `after an edit the Pay Later message is hidden and isn't loaded again`() = runTest(dispatcher) {
        loadAndSettle()

        completeReturn()
        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        sut.onRequestChanged(request("99.00"), showCreditMessaging = true)
        advanceUntilIdle()

        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        coVerify(exactly = 1) { client.fetchCreditPresentmentMessages(any(), any()) }
    }

    @Test
    fun `a Pay Later message still loading when an edit completes stays hidden`() = runTest(dispatcher) {
        val inFlight = CompletableDeferred<PayPalCreditMessagingResult>()
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } coAnswers { inFlight.await() }
        sut.load(request(AMOUNT), showCreditMessaging = true)
        runCurrent()

        completeReturn()
        inFlight.complete(MESSAGE_RESULT)
        advanceUntilIdle()

        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
    }

    @Test
    fun `only the brand shows when the edited funding instrument can't be loaded`() = runTest(dispatcher) {
        loadAndSettle()

        completeReturn(tokenized = success(null))

        assertEquals(PayPalSavedPaymentMethodDisplayState.Error, sut.state.value.fundingInstrument)
        coVerify(exactly = 0) { client.fetchPaymentMethod(BUYER_UPDATED, any(), any()) }
    }

    @Test
    fun `a failed load of the edited funding instrument shows the same state as a failed first load`() =
        runTest(dispatcher) {
            loadAndSettle()
            coEvery { client.fetchPaymentMethod(any(), ORDER_ID, any()) } throws UnknownHostException()

            completeReturn()

            assertEquals(PayPalSavedPaymentMethodDisplayState.NoNetwork, sut.state.value.fundingInstrument)
        }

    @Test
    fun `a failed load of the edited funding instrument isn't retried when a view attaches again`() =
        runTest(dispatcher) {
            loadAndSettle()
            coEvery { client.fetchPaymentMethod(any(), ORDER_ID, any()) } throws UnknownHostException()
            completeReturn()

            loadAndSettle()

            coVerify(exactly = 1) { client.fetchPaymentMethod(BUYER_DEFAULT, any(), any()) }
            coVerify(exactly = 1) { client.fetchPaymentMethod(BUYER_UPDATED, ORDER_ID, any()) }
        }

    @Test
    fun `an edit that completes while the first load is running wins over that load`() = runTest(dispatcher) {
        val firstLoad = CompletableDeferred<PayPalSavedPaymentMethodSummary>()
        coEvery { client.fetchPaymentMethod(any(), null, any()) } coAnswers { firstLoad.await() }
        sut.load(request(AMOUNT), showCreditMessaging = true)
        runCurrent()

        completeReturn()
        firstLoad.complete(summary(CARD))
        advanceUntilIdle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(EDITED), sut.state.value.fundingInstrument)
    }

    @Test
    fun `a failed return is delivered and completes the return`() = runTest(dispatcher) {
        val error = BraintreeException("return failed")
        every { launcher.handleReturnToApp(any(), any()) } returns authFailure(error)

        val handling = sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()

        assertEquals(PayPalReturnHandling.COMPLETED, handling)
        assertSame(error, firstFailureError())
    }

    @Test
    fun `a return that can't be restored fails and completes the return`() = runTest(dispatcher) {
        val error = IllegalStateException("bad pending request")
        every { launcher.restorePendingRequest(any()) } throws error

        val handling = sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()

        assertEquals(PayPalReturnHandling.COMPLETED, handling)
        assertSame(error, firstFailureError())
    }

    @Test
    fun `a return without an intent fails without completing the return`() = runTest(dispatcher) {
        val handling = sut.handleReturnToApp(PENDING, launcher, null)
        advanceUntilIdle()

        assertEquals(PayPalReturnHandling.TAKEN, handling)
        assertEquals(PayPalSavedPaymentMethodViewModel.NO_INTENT_MESSAGE, firstFailureError().message)
        assertNull(savedState.get<String>("completed_pending_request"))
    }

    @Test
    fun `a completed return is saved as a digest, not the pending request itself`() = runTest(dispatcher) {
        completeReturn()

        val saved = savedState.get<String>("completed_pending_request")
        assertNotEquals(PENDING, saved)
        assertTrue(saved.orEmpty().matches(Regex("[0-9a-f]{64}")))
        assertFalse(savedState.keys().any { savedState.get<Any>(it) == PENDING })
    }

    @Test
    fun `a repeat of a completed return in a recreated process is reported as completed without completing it`() =
        runTest(dispatcher) {
            completeReturn()

            val handling = newViewModel().handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
            advanceUntilIdle()

            assertEquals(PayPalReturnHandling.COMPLETED, handling)
            verify(exactly = 1) { launcher.handleReturnToApp(any(), any()) }
            coVerify(exactly = 1) { client.tokenize(any<PayPalPaymentAuthResult.Success>()) }
        }

    @Test
    fun `a cancel from tokenizing doesn't complete the return, so it can still be completed`() = runTest(dispatcher) {
        completeReturn(tokenized = PayPalResult.Cancel)
        assertSame(PayPalResult.Cancel, sut.results.first())

        val success = completeReturn()

        assertSame(success, sut.results.first())
    }

    @Test
    fun `a repeat of a completed return is reported as completed without reading it again`() =
        runTest(dispatcher) {
            completeReturn()

            val handling = sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))

            assertEquals(PayPalReturnHandling.COMPLETED, handling)
            verify(exactly = 1) { launcher.handleReturnToApp(any(), any()) }
        }

    @Test
    fun `a return dropped while an edit step runs tells the caller to keep its pending request`() =
        runTest(dispatcher) {
            val created = startEditInProgress()

            assertEquals(PayPalReturnHandling.NOT_TAKEN, sut.handleReturnToApp(PENDING, launcher, intentWith(null)))
            created.complete(PayPalSavedPaymentMethodAuthRequest.Failure(BraintreeException("failed")))
            advanceUntilIdle()

            verify(exactly = 0) { launcher.handleReturnToApp(any(), any()) }
        }

    @Test
    fun `a return interrupted while tokenizing isn't completed again`() = runTest(dispatcher) {
        val authResult = mockk<PayPalPaymentAuthResult.Success>()
        every { launcher.handleReturnToApp(any(), any()) } returns authResult
        coEvery { client.tokenize(authResult) } coAnswers { CompletableDeferred<PayPalResult>().await() }
        sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
        runCurrent()

        newViewModel().handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
        runCurrent()

        verify(exactly = 1) { launcher.handleReturnToApp(any(), any()) }
    }

    @Test
    fun `an edit interrupted by process death shows the original funding instrument and can be started again`() =
        runTest(dispatcher) {
            val authResult = mockk<PayPalPaymentAuthResult.Success>()
            every { launcher.handleReturnToApp(any(), any()) } returns authResult
            coEvery { client.tokenize(authResult) } coAnswers { CompletableDeferred<PayPalResult>().await() }
            sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
            runCurrent()

            sut = newViewModel()
            loadAndSettle()
            val pending = PayPalPendingRequest.Started("another-pending")
            coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } returns
                PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch(mockk())
            every { launcher.launch(any(), any()) } returns pending

            assertFalse(sut.state.value.isEditCompleted)
            assertEquals(PayPalSavedPaymentMethodDisplayState.Content(CARD), sut.state.value.fundingInstrument)
            assertSame(pending, sut.startEdit(activity, launcher, CHECKOUT))
        }

    @Test
    fun `a return with no result is delivered as a cancel without completing the return`() = runTest(dispatcher) {
        every { launcher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult

        val handling = sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()

        assertEquals(PayPalReturnHandling.TAKEN, handling)
        assertSame(PayPalResult.Cancel, sut.results.first())
        assertFalse(sut.state.value.isEditCompleted)
    }

    @Test
    fun `a return with no result can be completed later`() = runTest(dispatcher) {
        every { launcher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult
        sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()
        sut.results.first()

        val success = completeReturn()

        assertSame(success, sut.results.first())
    }

    @Test
    fun `a result whose delivery is cancelled reaches the next view`() = runTest(dispatcher) {
        val success = completeReturn()
        val handling = CompletableDeferred<Unit>()
        val rotatedView = launch { sut.results.collect { handling.await() } }
        runCurrent()

        rotatedView.cancel()
        runCurrent()

        assertSame(success, sut.results.first())
    }

    @Test
    fun `a handled result isn't delivered again`() = runTest(dispatcher) {
        completeReturn()
        sut.results.first()

        val next = async { sut.results.first() }
        runCurrent()

        assertFalse(next.isCompleted)
        next.cancel()
    }

    @Test
    fun `results sent while no view collects are delivered in order`() = runTest(dispatcher) {
        completeReturn(tokenized = PayPalResult.Cancel)
        val success = completeReturn()

        assertEquals(listOf(PayPalResult.Cancel, success), sut.results.take(2).toList())
    }

    @Test
    fun `an edit saved before the process died loads the funding instrument the buyer picked`() = runTest(dispatcher) {
        savedState["is_edit_completed"] = true
        savedState["edited_order_id"] = ORDER_ID
        sut = newViewModel()

        loadAndSettle()

        assertEquals(PayPalSavedPaymentMethodDisplayState.Content(EDITED), sut.state.value.fundingInstrument)
        assertEquals(PayPalCreditMessagingDisplayState.Hidden, sut.state.value.creditMessaging)
        coVerify(exactly = 0) { client.fetchPaymentMethod(any(), null, any()) }
        coVerify(exactly = 0) { client.fetchCreditPresentmentMessages(any(), any()) }
    }

    @Test
    fun `the factory works without a saved state owner, keeping nothing across process death`() =
        runTest(dispatcher) {
            val factory = PayPalSavedPaymentMethodViewModel.Factory { client }

            sut = factory.create(PayPalSavedPaymentMethodViewModel::class.java, CreationExtras.Empty)
            loadAndSettle()

            assertFalse(sut.state.value.isEditCompleted)
            assertEquals(PayPalSavedPaymentMethodDisplayState.Content(CARD), sut.state.value.fundingInstrument)
        }

    private fun newViewModel() = PayPalSavedPaymentMethodViewModel(savedState, client)

    private fun TestScope.loadAndSettle(amount: String = AMOUNT, showCreditMessaging: Boolean = true) {
        sut.load(request(amount), showCreditMessaging)
        advanceUntilIdle()
    }

    // Returns from PayPal with [tokenized] as the result and waits for it to be handled.
    private fun TestScope.completeReturn(tokenized: PayPalResult = success(ORDER_ID)): PayPalResult {
        givenReturn(tokenized)
        sut.handleReturnToApp(PENDING, launcher, intentWith(appLinkReturnUrl))
        advanceUntilIdle()
        return tokenized
    }

    // Leaves an edit step running until the returned auth request is completed.
    private fun TestScope.startEditInProgress(): CompletableDeferred<PayPalSavedPaymentMethodAuthRequest> {
        val created = CompletableDeferred<PayPalSavedPaymentMethodAuthRequest>()
        coEvery { client.createEditFundingInstrumentAuthRequest(any(), any()) } coAnswers { created.await() }
        backgroundScope.launch { sut.startEdit(activity, launcher, CHECKOUT) }
        runCurrent()
        return created
    }

    private suspend fun firstFailureError(): Exception = (sut.results.first() as PayPalResult.Failure).error

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

    private fun request(amount: String) = PayPalSavedPaymentMethodRequest(amount, "USD", MERCHANT_ACCOUNT_ID)

    private fun summary(paymentMethod: PayPalSavedPaymentMethod) =
        PayPalSavedPaymentMethodSummary(listOf(paymentMethod), PayPalPayer("buyer@example.com", isEditable = true))

    private companion object {
        const val PENDING = "pending"
        const val ORDER_ID = "order-1"
        const val MERCHANT_ACCOUNT_ID = "ma-1"
        const val AMOUNT = "55.00"
        val BUYER_DEFAULT = PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT
        val BUYER_UPDATED = PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT
        val CHECKOUT = PayPalCheckoutRequest(amount = AMOUNT, hasUserLocationConsent = true)
        val CARD = PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.CARD, "Visa", null, "1234", null)
        val EDITED = PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.BANK, "Chase", null, "9876", null)
        val MESSAGE_RESULT = messageResult("Pay in 4")
        val MESSAGE = message("Pay in 4")

        fun messageResult(text: String) = PayPalCreditMessagingResult(
            mainItems = listOf(PayPalCreditMessageItem(null, text, null, null, null, null, null)),
            disclaimerItems = emptyList(),
            actionItems = emptyList(),
            messageId = null,
            messageType = null,
            impressionUrl = null
        )

        fun message(text: String) =
            PayPalCreditMessagingDisplayState.Content(PayPalCreditMessagingContent(text, learnMore = null))
    }
}
