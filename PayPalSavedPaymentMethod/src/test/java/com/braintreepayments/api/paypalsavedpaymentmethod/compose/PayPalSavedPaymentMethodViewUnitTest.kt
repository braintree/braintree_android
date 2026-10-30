@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodViewModel
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodViewUnitTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val state = MutableStateFlow(
        PayPalSavedPaymentMethodUiState(
            fundingInstrument = CARD_STATE,
            creditMessaging = PayPalCreditMessagingDisplayState.Hidden
        )
    )
    private val results = MutableSharedFlow<PayPalResult>(extraBufferCapacity = 1)
    private val viewModel = mockk<PayPalSavedPaymentMethodViewModel>(relaxed = true)
    private val pendingRequestRepository = mockk<PendingRequestRepository>(relaxed = true)
    private val authorizations = mutableListOf<String>()
    private val delivered = mutableListOf<PayPalResult>()
    private val inputs = mutableStateOf(Inputs())

    @Before
    fun setUp() {
        every { viewModel.state } returns state
        every { viewModel.results } returns results
        coEvery { pendingRequestRepository.get() } returns null
        coEvery { pendingRequestRepository.store(any()) } returns true
        composeTestRule.setContent {
            val current = inputs.value
            PayPalSavedPaymentMethodView(
                payPalCheckoutRequest = CHECKOUT,
                request = current.request,
                authorization = current.authorization,
                appLinkReturnUrl = RETURN_URL,
                modifier = Modifier,
                deepLinkFallbackUrlScheme = null,
                style = PayPalSavedPaymentMethodViewStyle(),
                paypalTokenizeCallback = { delivered += it },
                viewModelFactory = { authorization, _, _ ->
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                            authorizations += authorization
                            return viewModel as T
                        }
                    }
                },
                createPendingRequestRepository = { pendingRequestRepository }
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `loads the request once and shows the view model's state`() {
        verify(exactly = 1) { viewModel.load(REQUEST, true) }
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG)
            .assertContentDescriptionEquals("Visa, ending in 1 2 3 4")
    }

    @Test
    fun `a new request is passed to the view model`() {
        inputs.value = inputs.value.copy(request = PayPalSavedPaymentMethodRequest("20.00", "USD"))
        composeTestRule.waitForIdle()

        verify { viewModel.onRequestChanged(match { it.amount == "20.00" }, true) }
        verify(exactly = 1) { viewModel.load(any(), any()) }
    }

    @Test
    fun `a new authorization gets a new view model, the same one keeps it`() {
        inputs.value = inputs.value.copy(request = PayPalSavedPaymentMethodRequest("20.00", "USD"))
        composeTestRule.waitForIdle()
        assertEquals(listOf("client-token"), authorizations)

        inputs.value = inputs.value.copy(authorization = "another-client-token")
        composeTestRule.waitForIdle()

        assertEquals(listOf("client-token", "another-client-token"), authorizations)
    }

    @Test
    fun `tapping the chip opens the paysheet, and the next resume completes the return`() {
        coEvery { viewModel.startEdit(any(), any(), any()) } returns PayPalPendingRequest.Started("pending")

        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).performClick()
        composeTestRule.waitForIdle()
        coVerify { pendingRequestRepository.store("pending") }

        resumeAgain()

        verify { viewModel.handleReturnToApp("pending", any(), any()) }
    }

    @Test
    fun `a failed paysheet launch is reported to the callback`() {
        val error = BraintreeException("launch failed")
        coEvery { viewModel.startEdit(any(), any(), any()) } returns PayPalPendingRequest.Failure(error)

        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        assertSame(error, (delivered.single() as PayPalResult.Failure).error)
    }

    @Test
    fun `a result is delivered to the callback and clears the pending request`() {
        coEvery { viewModel.startEdit(any(), any(), any()) } returns PayPalPendingRequest.Started("pending")
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        results.tryEmit(PayPalResult.Cancel)
        composeTestRule.waitForIdle()
        resumeAgain()

        assertEquals(listOf<PayPalResult>(PayPalResult.Cancel), delivered)
        coVerify { pendingRequestRepository.clear() }
        verify(exactly = 0) { viewModel.handleReturnToApp(any(), any(), any()) }
    }

    @Test
    fun `an edit in progress disables the chip and shows the loader`() {
        // The loader's spinner animates forever, so the clock is advanced by hand.
        composeTestRule.mainClock.autoAdvance = false

        state.value = state.value.copy(isEditing = true)
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Loading").assertExists()
    }

    // Pauses and resumes the activity, as when the buyer comes back from the paysheet.
    private fun resumeAgain() {
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()
    }

    private data class Inputs(
        val authorization: String = "client-token",
        val request: PayPalSavedPaymentMethodRequest = REQUEST
    )

    private companion object {
        val RETURN_URL: Uri = Uri.parse("https://merchant.example.com")
        val REQUEST = PayPalSavedPaymentMethodRequest("10.00", "USD")
        val CHECKOUT = PayPalCheckoutRequest(amount = "10.00", hasUserLocationConsent = true)
        val CARD_STATE = PayPalSavedPaymentMethodDisplayState.Content(
            PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.CARD, "Visa", null, "1234", null)
        )
    }
}
