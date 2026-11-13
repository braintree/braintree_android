@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodReturnResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodViewModel
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodViewUnitTest {

    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val state = MutableStateFlow(PayPalSavedPaymentMethodUiState())
    private val results = MutableSharedFlow<PayPalResult>(extraBufferCapacity = 1)
    private val viewModel = mockk<PayPalSavedPaymentMethodViewModel>(relaxed = true)
    private var createdViewModels = 0
    private val authorizations = mutableListOf<String>()
    private val launchedRequests = mutableListOf<PayPalPendingRequest.Started>()
    private val delivered = mutableListOf<PayPalResult>()
    private var sut = newView()

    init {
        every { viewModel.state } returns state
        every { viewModel.results } returns results
        every { viewModel.handleReturnToApp(any(), any(), any()) } returns
            PayPalSavedPaymentMethodReturnResult.TAKEN
    }

    @Test
    fun `initialize configures the view model, loads the request and shows the state`() {
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE, creditMessaging = MESSAGE_STATE)
        idle()

        assertEquals(listOf("client-token"), authorizations)
        verify { viewModel.load(match { it.amount == "10.00" }, true) }
        assertEquals("••1234", chipText().text.toString())
        assertTrue(creditMessaging().isVisible)
    }

    @Test
    fun `a new request or turning the Pay Later message back on is passed to the view model`() {
        sut.style = PayPalSavedPaymentMethodViewStyle(showPayPalCreditMessaging = false)
        initialize()

        sut.request = PayPalSavedPaymentMethodRequest("20.00", "USD", "ma-1")
        sut.style = PayPalSavedPaymentMethodViewStyle(showPayPalCreditMessaging = true)

        verify { viewModel.onRequestChanged(match { it.amount == "20.00" }, false) }
        verify { viewModel.onRequestChanged(match { it.amount == "20.00" }, true) }
    }

    @Test
    fun `tapping the funding instrument passes the pending request to the launch callback`() {
        val pending = PayPalPendingRequest.Started("pending")

        tapChip(pending)

        assertEquals(listOf(pending), launchedRequests)
    }

    @Test
    fun `a failed edit launch is reported to the tokenize callback`() {
        val error = BraintreeException("launch failed")

        tapChip(PayPalPendingRequest.Failure(error))

        assertSame(error, (delivered.single() as PayPalResult.Failure).error)
    }

    @Test
    fun `handleReturnToApp completes the edit, and its result reaches the tokenize callback`() {
        initialize()
        val intent = Intent()

        sut.handleReturnToApp(PayPalPendingRequest.Started("pending"), intent)
        results.tryEmit(PayPalResult.Cancel)
        idle()

        verify { viewModel.handleReturnToApp("pending", any(), intent) }
        assertEquals(listOf<PayPalResult>(PayPalResult.Cancel), delivered)
    }

    @Test
    fun `handleReturnToApp tells the merchant whether to keep the pending request, and clears a used-up return URL`() {
        initialize()
        val pending = PayPalPendingRequest.Started("pending")
        val returnUrl = RETURN_URL.buildUpon().path("return").build()

        // What the view model did, whether the merchant can forget the request, and the intent's data afterwards.
        listOf(
            Triple(PayPalSavedPaymentMethodReturnResult.NOT_TAKEN, false, returnUrl),
            Triple(PayPalSavedPaymentMethodReturnResult.TAKEN, true, returnUrl),
            Triple(PayPalSavedPaymentMethodReturnResult.COMPLETED, true, null)
        ).forEach { (result, canForget, dataAfter) ->
            every { viewModel.handleReturnToApp(any(), any(), any()) } returns result
            val intent = Intent(Intent.ACTION_VIEW, returnUrl)
            assertEquals(canForget, sut.handleReturnToApp(pending, intent))
            assertEquals(dataAfter, intent.data)
        }
    }

    @Test
    fun `handleReturnToApp does nothing before initialize`() {
        val taken = sut.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent())

        assertEquals(false, taken)
        verify(exactly = 0) { viewModel.handleReturnToApp(any(), any(), any()) }
    }

    @Test
    fun `an edit in progress shows the loader and disables the chip`() {
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE, isEditing = true)
        idle()
        assertTrue(ShadowDialog.getLatestDialog().isShowing)
        assertFalse(chip().isEnabled)

        state.value = state.value.copy(isEditing = false)
        idle()
        assertFalse(ShadowDialog.getLatestDialog().isShowing)
        assertTrue(chip().isEnabled)
    }

    @Test
    fun `a style change while detached during an edit doesn't open the loader again`() {
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE, isEditing = true)
        idle()
        activity.setContentView(View(activity))
        val shown = ShadowDialog.getShownDialogs().size

        sut.style = PayPalSavedPaymentMethodViewStyle(showPayPalCreditMessaging = false)

        assertEquals(shown, ShadowDialog.getShownDialogs().size)
    }

    @Test
    fun `after being detached and attached again the view shows the latest state`() {
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE, isEditing = true)
        idle()

        activity.setContentView(View(activity))
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE)
        activity.setContentView(sut)
        idle()

        assertTrue(chip().isEnabled)
    }

    @Test
    fun `views with different ids get their own view model, the same id gets the same one`() {
        initialize()
        val firstId = sut.id

        sut = newView()
        initialize()
        assertEquals(2, createdViewModels)

        sut = newView().apply { id = firstId }
        initialize()
        assertEquals(2, createdViewModels)
    }

    @Test
    fun `outside an activity with an ActivityResultRegistry it reports a failure and hides`() {
        sut = PayPalSavedPaymentMethodView(RuntimeEnvironment.getApplication())

        initialize()

        assertTrue(delivered.single() is PayPalResult.Failure)
        assertFalse(sut.getChildAt(0).isVisible)
        assertEquals(0, createdViewModels)
    }

    @Test
    fun `without an id it reports a failure and hides`() {
        sut = PayPalSavedPaymentMethodView(activity).apply { activity.setContentView(this) }

        initialize()

        assertEquals("PayPalSavedPaymentMethodView needs an android:id.", failureMessage())
        assertFalse(sut.getChildAt(0).isVisible)
        assertEquals(0, createdViewModels)
    }

    @Test
    fun `an id another attached view already uses reports a failure and hides`() {
        initialize()
        val first = sut
        sut = PayPalSavedPaymentMethodView(activity).apply { id = first.id }
        (first.parent as ViewGroup).addView(sut)

        initialize()

        assertEquals(true, failureMessage()?.startsWith("Another PayPalSavedPaymentMethodView"))
        assertFalse(sut.getChildAt(0).isVisible)
        verify(exactly = 1) { viewModel.load(any(), any()) }
    }

    @Test
    fun `outside a view tree the view model is found on attach, and a return handled before then completes`() {
        sut = PayPalSavedPaymentMethodView(activity).apply { id = View.generateViewId() }
        initialize()
        val intent = Intent()
        sut.handleReturnToApp(PayPalPendingRequest.Started("pending"), intent)
        assertEquals(0, createdViewModels)

        activity.setContentView(sut)
        idle()

        assertEquals(1, createdViewModels)
        assertEquals(listOf("client-token"), authorizations)
        verify { viewModel.handleReturnToApp("pending", any(), intent) }
    }

    @Test
    fun `initializing again keeps the launcher that opened the paysheet`() {
        val launchers = mutableListOf<PayPalLauncher>()
        every { viewModel.handleReturnToApp(any(), capture(launchers), any()) } returns
            PayPalSavedPaymentMethodReturnResult.TAKEN
        initialize()
        sut.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent())

        initialize()
        sut.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent())

        assertSame(launchers[0], launchers[1])
    }

    @Test
    fun `a refreshed authorization keeps the view model, as on iOS`() {
        initialize()

        initialize(authorization = "refreshed-client-token")

        assertEquals(1, createdViewModels)
        assertEquals(listOf("client-token"), authorizations)
    }

    @Test
    fun `in a window without a view model owner, initializing after attach uses the activity's`() {
        sut = PayPalSavedPaymentMethodView(activity).apply { id = View.generateViewId() }
        Dialog(activity).apply {
            setContentView(sut)
            show()
        }
        idle()

        initialize()
        sut.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent())

        assertEquals(listOf("client-token"), authorizations)
        verify { viewModel.handleReturnToApp("pending", any(), any()) }
    }

    private fun failureMessage(): String? = (delivered.single() as PayPalResult.Failure).error.message

    private fun newView() = PayPalSavedPaymentMethodView(activity).apply {
        id = View.generateViewId()
        activity.setContentView(this)
    }

    private fun initialize(authorization: String = "client-token") {
        sut.viewModelFactory = { factoryAuthorization, _, _ ->
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    createdViewModels++
                    authorizations += factoryAuthorization
                    return viewModel as T
                }
            }
        }
        sut.initialize(
            authorization = authorization,
            appLinkReturnUrl = RETURN_URL,
            payPalCheckoutRequest = PayPalCheckoutRequest(amount = "10.00", hasUserLocationConsent = true),
            request = PayPalSavedPaymentMethodRequest("10.00", "USD", "ma-1"),
            launchCallback = { launchedRequests += it },
            tokenizeCallback = { delivered += it }
        )
        idle()
    }

    // Initializes with a card and taps it; the edit returns [pending].
    private fun tapChip(pending: PayPalPendingRequest) {
        coEvery { viewModel.startEdit(any(), any(), any()) } returns pending
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE)
        idle()
        chip().performClick()
        idle()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun chip(): View = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_chip)
    private fun chipText(): TextView = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_text)
    private fun creditMessaging(): View = sut.findViewById(R.id.paypal_saved_payment_method_credit_messaging)

    private companion object {
        val RETURN_URL: Uri = Uri.parse("https://merchant.example.com")
        val CARD_STATE = PayPalSavedPaymentMethodDisplayState.Content(
            PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.CARD, "Visa", null, "1234", null)
        )
        val MESSAGE_STATE = PayPalCreditMessagingDisplayState.Content(PayPalCreditMessagingContent("Pay in 4.", null))
    }
}
