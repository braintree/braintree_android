@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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

    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    private val activity = activityController.get()
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
        every { viewModel.resultKey } returns "result-key"
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
    fun `each rendered state is reported to the view model with the style`() {
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE)
        idle()

        verify { viewModel.onRendered(sut.style) }
    }

    @Test
    fun `tapping Learn more reports it to the view model`() {
        initialize()
        val contentView = sut.getChildAt(0) as PayPalSavedPaymentMethodContentView

        contentView.onLearnMoreClick(PayPalCreditMessagingContent.LearnMore("Learn more", LEARN_MORE_URL, false))

        verify { viewModel.onLearnMoreSelected() }
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
    fun `the host's lifecycle ending cancels an edit being set up, even before the view detaches`() {
        val host = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        sut.setViewTreeLifecycleOwner(host)
        var cancelled = false
        coEvery { viewModel.startEdit(any(), any(), any(), any()) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE)
        idle()
        chip().performClick()
        idle()

        host.lifecycle.currentState = Lifecycle.State.DESTROYED
        idle()

        assertTrue(cancelled)
        assertTrue(delivered.isEmpty())
    }

    @Test
    fun `detaching while the edit is being set up cancels it and calls neither callback`() {
        var cancelled = false
        coEvery { viewModel.startEdit(any(), any(), any(), any()) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE)
        idle()
        chip().performClick()
        idle()

        activity.setContentView(View(activity))
        idle()

        assertTrue(cancelled)
        assertTrue(launchedRequests.isEmpty())
        assertTrue(delivered.isEmpty())
    }

    @Test
    fun `back while the edit is being set up cancels it and reports a cancel`() {
        var cancelled = false
        coEvery { viewModel.startEdit(any(), any(), any(), any()) } coAnswers {
            state.value = state.value.copy(isEditing = true)
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE)
        idle()
        chip().performClick()
        idle()

        pressBack(ShadowDialog.getLatestDialog())
        idle()

        assertTrue(cancelled)
        assertEquals(listOf<PayPalResult>(PayPalResult.Cancel), delivered)
    }

    @Test
    fun `back doesn't close the loader while the approved edit is being completed`() {
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE, isEditing = true)
        idle()

        val loader = ShadowDialog.getLatestDialog()
        pressBack(loader)

        assertTrue(loader.isShowing)
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
    fun `a return with no PayPal result is delivered as a cancel, and the merchant can forget the request`() {
        initialize()
        every { viewModel.handleReturnToApp(any(), any(), any()) } answers {
            results.tryEmit(PayPalResult.Cancel)
            PayPalSavedPaymentMethodReturnResult.TAKEN
        }

        val canForget = sut.handleReturnToApp(PayPalPendingRequest.Started("pending"), Intent())
        idle()

        assertTrue(canForget)
        assertEquals(listOf<PayPalResult>(PayPalResult.Cancel), delivered)
    }

    @Test
    fun `rotating during an edit closes the loader and the new view shows it again`() {
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE, isEditing = true)
        idle()
        val before = ShadowDialog.getLatestDialog()
        assertTrue(before.isShowing)

        // The old view goes with the old activity; a new one with the same id binds the same view model.
        activity.setContentView(View(activity))
        val recreated = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val sameId = sut.id
        sut = PayPalSavedPaymentMethodView(recreated).apply {
            id = sameId
            recreated.setContentView(this)
        }
        initialize()

        assertFalse(before.isShowing)
        val after = ShadowDialog.getLatestDialog()
        assertTrue(after !== before && after.isShowing)
    }

    @Test
    fun `a repeat initialize with a changed request passes it to the view model once`() {
        initialize()

        initialize(request = PayPalSavedPaymentMethodRequest("20.00", "USD", "ma-1"))

        verify(exactly = 1) { viewModel.onRequestChanged(match { it.amount == "20.00" }, true) }
    }

    @Test
    fun `the launcher registers under the same key after the activity is recreated`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        controller.get().showRealView(RECREATED_VIEW_ID)
        val before = controller.get().savedPaymentMethodResultKeys()

        controller.recreate()
        controller.get().showRealView(RECREATED_VIEW_ID)

        assertEquals(1, before.size)
        assertEquals(before, controller.get().savedPaymentMethodResultKeys())
    }

    @Test
    fun `same-id views under different view model owners register different result keys`() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val container = LinearLayout(host).also { host.setContentView(it) }

        repeat(2) {
            val owner = object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
            container.addView(PayPalSavedPaymentMethodView(host).apply {
                id = RECREATED_VIEW_ID
                setViewTreeViewModelStoreOwner(owner)
            })
        }
        (0 until container.childCount).forEach { (container.getChildAt(it) as PayPalSavedPaymentMethodView).setUp() }

        assertEquals(2, host.savedPaymentMethodResultKeys().toSet().size)
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
    fun `outside a ComponentActivity it reports a failure and hides`() {
        sut = PayPalSavedPaymentMethodView(RuntimeEnvironment.getApplication()).apply { activity.setContentView(this) }

        initialize()

        assertTrue(delivered.single() is PayPalResult.Failure)
        assertFalse(sut.getChildAt(0).isVisible)
        assertEquals(0, createdViewModels)
    }

    @Test
    fun `a setup failure before the view is attached is reported once it attaches`() {
        sut = PayPalSavedPaymentMethodView(activity)

        initialize()
        assertTrue(delivered.isEmpty())

        activity.setContentView(sut)
        idle()

        assertEquals("PayPalSavedPaymentMethodView needs an android:id.", failureMessage())
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
    fun `a setup failure reaches the callback after initialize returns, not from inside it`() {
        sut = PayPalSavedPaymentMethodView(activity).apply { activity.setContentView(this) }

        initialize(settle = false)
        assertTrue(delivered.isEmpty())

        idle()
        assertEquals("PayPalSavedPaymentMethodView needs an android:id.", failureMessage())
    }

    @Test
    fun `a setup failure isn't delivered once the view is detached`() {
        sut = PayPalSavedPaymentMethodView(activity).apply { activity.setContentView(this) }
        initialize(settle = false)

        activity.setContentView(View(activity))
        idle()

        assertTrue(delivered.isEmpty())
    }

    @Test
    fun `a setup failure isn't delivered once the activity is destroyed`() {
        sut = PayPalSavedPaymentMethodView(activity).apply { activity.setContentView(this) }
        initialize(settle = false)

        activityController.pause().stop().destroy()
        idle()

        assertTrue(delivered.isEmpty())
    }

    @Test
    fun `initializing again before a setup failure is delivered reports it once`() {
        sut = PayPalSavedPaymentMethodView(activity).apply { activity.setContentView(this) }

        initialize(settle = false)
        initialize()

        assertEquals("PayPalSavedPaymentMethodView needs an android:id.", failureMessage())
    }

    @Test
    fun `outside a view tree the view model is found on attach, and a return handled before then completes`() {
        sut = PayPalSavedPaymentMethodView(activity).apply { id = View.generateViewId() }
        initialize()
        val intent = Intent()
        // Not taken yet, so the merchant keeps the pending request in case the process dies before the view attaches.
        assertFalse(sut.handleReturnToApp(PayPalPendingRequest.Started("pending"), intent))
        assertEquals(0, createdViewModels)

        activity.setContentView(sut)
        idle()

        assertEquals(1, createdViewModels)
        assertEquals(listOf("client-token"), authorizations)
        verify { viewModel.handleReturnToApp("pending", any(), intent) }
    }

    @Test
    fun `initializing again keeps observing the view model, so a result is delivered once`() {
        var collections = 0
        every { viewModel.results } returns flow {
            collections++
            emitAll(results)
        }
        initialize()
        initialize()

        results.tryEmit(PayPalResult.Cancel)
        idle()

        assertEquals(1, collections)
        assertEquals(listOf<PayPalResult>(PayPalResult.Cancel), delivered)
    }

    @Test
    fun `a result while the activity is stopped is delivered once it starts again`() {
        // Replayed, as the view model keeps a result until a view handles it.
        val kept = MutableSharedFlow<PayPalResult>(replay = 1)
        every { viewModel.results } returns kept
        initialize()
        activityController.pause().stop()

        kept.tryEmit(PayPalResult.Cancel)
        idle()
        assertTrue(delivered.isEmpty())

        activityController.start().resume()
        idle()
        assertEquals(listOf<PayPalResult>(PayPalResult.Cancel), delivered)
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
    fun `a refreshed authorization keeps the view model`() {
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

    private fun initialize(
        authorization: String = "client-token",
        settle: Boolean = true,
        request: PayPalSavedPaymentMethodRequest = PayPalSavedPaymentMethodRequest("10.00", "USD", "ma-1")
    ) {
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
            request = request,
            launchCallback = { launchedRequests += it },
            tokenizeCallback = { delivered += it }
        )
        if (settle) idle()
    }

    // Initializes with a card and taps it; the edit returns [pending].
    private fun tapChip(pending: PayPalPendingRequest) {
        coEvery { viewModel.startEdit(any(), any(), any(), any()) } returns pending
        initialize()
        state.value = PayPalSavedPaymentMethodUiState(fundingInstrument = CARD_STATE)
        idle()
        chip().performClick()
        idle()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun pressBack(dialog: Dialog) {
        dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
    }

    private fun chip(): View = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_chip)
    private fun chipText(): TextView = sut.findViewById(R.id.paypal_saved_payment_method_funding_instrument_text)
    private fun creditMessaging(): View = sut.findViewById(R.id.paypal_saved_payment_method_credit_messaging)

    private companion object {
        val RETURN_URL: Uri = Uri.parse("https://merchant.example.com")
        const val LEARN_MORE_URL = "https://www.paypal.com/learn-more"
        val CARD_STATE = PayPalSavedPaymentMethodDisplayState.Content(
            PayPalSavedPaymentMethod(PayPalSavedPaymentMethodType.CARD.rawValue, "Visa", null, "1234", null)
        )
        val MESSAGE_STATE = PayPalCreditMessagingDisplayState.Content(PayPalCreditMessagingContent("Pay in 4.", null))
    }
}

// The saved payment method keys registered in the activity's result registry, read from its saved state.
private fun ComponentActivity.savedPaymentMethodResultKeys(): List<String> {
    val saved = Bundle()
    activityResultRegistry.onSaveInstanceState(saved)
    return saved.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS").orEmpty()
        .filter { it.startsWith("PayPalSavedPaymentMethodView:") }
}

// Shows a view with the real view model, as a merchant would.
private fun ComponentActivity.showRealView(viewId: Int) {
    val view = PayPalSavedPaymentMethodView(this).apply { id = viewId }
    setContentView(view)
    view.setUp()
}

private fun PayPalSavedPaymentMethodView.setUp() = initialize(
    authorization = "client-token",
    appLinkReturnUrl = Uri.parse("https://merchant.example.com"),
    payPalCheckoutRequest = PayPalCheckoutRequest(amount = "10.00", hasUserLocationConsent = true),
    request = PayPalSavedPaymentMethodRequest("10.00", "USD"),
    launchCallback = { },
    tokenizeCallback = { }
)

private const val RECREATED_VIEW_ID = 4242
