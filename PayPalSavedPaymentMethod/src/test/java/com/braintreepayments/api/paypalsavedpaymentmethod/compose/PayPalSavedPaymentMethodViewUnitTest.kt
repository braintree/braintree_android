@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodReturnResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodViewModel
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodViewUnitTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val restorationTester = StateRestorationTester(composeTestRule)
    private val state = MutableStateFlow(
        PayPalSavedPaymentMethodUiState(
            fundingInstrument = CARD_STATE,
            creditMessaging = PayPalCreditMessagingDisplayState.Hidden
        )
    )
    private val results = MutableSharedFlow<PayPalResult>(extraBufferCapacity = 1)
    private val viewModel = mockk<PayPalSavedPaymentMethodViewModel>(relaxed = true)
    private val authorizations = mutableListOf<String>()
    private val delivered = mutableListOf<PayPalResult>()
    private val registry = spyk(
        object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) = Unit
        }
    )
    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = registry
    }
    // Never equal, so setting equal inputs still recomposes, as a parent passing a new but equal request would.
    private val inputs = mutableStateOf(Inputs(), neverEqualPolicy())

    @Before
    fun setUp() {
        every { viewModel.state } returns state
        every { viewModel.results } returns results
        every { viewModel.handleReturnToApp(any(), any(), any()) } returns PayPalSavedPaymentMethodReturnResult.TAKEN
        restorationTester.setContent {
            val current = inputs.value
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
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
                    }
                )
            }
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
    fun `an equal request in a new instance doesn't reload the Pay Later message`() {
        val equalRequest = PayPalSavedPaymentMethodRequest("10.00", "USD")
        assertNotSame(REQUEST, equalRequest)

        inputs.value = inputs.value.copy(request = equalRequest)
        composeTestRule.waitForIdle()

        verify(exactly = 1) { viewModel.onRequestChanged(any(), any()) }
    }

    @Test
    fun `a refreshed authorization keeps the view model, as on iOS`() {
        inputs.value = inputs.value.copy(authorization = "refreshed-client-token")
        composeTestRule.waitForIdle()

        assertEquals(listOf("client-token"), authorizations)
        verify(exactly = 1) { viewModel.load(any(), any()) }
    }

    @Test
    fun `a refreshed authorization keeps the pending request, so the return is still completed`() {
        // Not taken the first time, so the pending request is still kept when the authorization changes.
        every { viewModel.handleReturnToApp("pending", any(), any()) } returnsMany
            listOf(PayPalSavedPaymentMethodReturnResult.NOT_TAKEN, PayPalSavedPaymentMethodReturnResult.TAKEN)
        startEdit()
        resumeAgain()

        inputs.value = inputs.value.copy(authorization = "refreshed-client-token")
        composeTestRule.waitForIdle()
        resumeAgain()

        verify(exactly = 2) { viewModel.handleReturnToApp("pending", any(), any()) }
    }

    @Test
    fun `tapping the chip opens the paysheet, and the next resume completes the return`() {
        startEdit()

        resumeAgain()

        verify { viewModel.handleReturnToApp("pending", any(), any()) }
    }

    @Test
    fun `a failed paysheet launch is reported to the callback`() {
        val error = BraintreeException("launch failed")
        startEdit(PayPalPendingRequest.Failure(error))

        assertSame(error, (delivered.single() as PayPalResult.Failure).error)
    }

    @Test
    fun `a result is delivered to the callback, and later resumes don't complete the return again`() {
        startEdit()

        results.tryEmit(PayPalResult.Cancel)
        composeTestRule.waitForIdle()
        resumeAgain()

        assertEquals(listOf<PayPalResult>(PayPalResult.Cancel), delivered)
        verify(exactly = 0) { viewModel.handleReturnToApp(any(), any(), any()) }
    }

    @Test
    fun `a failed tokenize is delivered to the callback, and later resumes don't pass the request again`() {
        val error = BraintreeException("tokenize failed")
        startEdit()

        results.tryEmit(PayPalResult.Failure(error))
        composeTestRule.waitForIdle()
        resumeAgain()

        assertSame(error, (delivered.single() as PayPalResult.Failure).error)
        verify(exactly = 0) { viewModel.handleReturnToApp(any(), any(), any()) }
    }

    @Test
    fun `a result while the activity is stopped is delivered once it starts again`() {
        // Replayed, as the view model keeps a result until a view handles it.
        val kept = MutableSharedFlow<PayPalResult>(replay = 1)
        every { viewModel.results } returns kept
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        kept.tryEmit(PayPalResult.Cancel)
        composeTestRule.waitForIdle()
        assertTrue(delivered.isEmpty())

        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()
        assertEquals(listOf<PayPalResult>(PayPalResult.Cancel), delivered)
    }

    @Test
    fun `a pending request survives restoring the saved state, and the return is completed with it`() {
        startEdit()
        verify(exactly = 0) { viewModel.handleReturnToApp(any(), any(), any()) }

        // Only saved state keeps the pending request, so it must come back with it.
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        verify { viewModel.handleReturnToApp("pending", any(), any()) }
    }

    @Test
    fun `the browser switch result key survives restoring the saved state`() {
        val keyBeforeRestore = registeredKeys().single()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // A new launcher registers again with the same key, so an Auth Tab result reaches it.
        assertEquals(listOf(keyBeforeRestore, keyBeforeRestore), registeredKeys())
    }

    @Test
    fun `a pending request the view model drops is passed again on the next resume, then forgotten`() {
        every { viewModel.handleReturnToApp("pending", any(), any()) } returnsMany
            listOf(PayPalSavedPaymentMethodReturnResult.NOT_TAKEN, PayPalSavedPaymentMethodReturnResult.TAKEN)
        startEdit()

        resumeAgain()
        resumeAgain()
        resumeAgain()

        verify(exactly = 2) { viewModel.handleReturnToApp("pending", any(), any()) }
    }

    @Test
    fun `a completed return clears the PayPal return URL from the activity's intent`() {
        // The return URL arrives with the return. The activity's intent isn't replaced, since the test tracks the
        // activity by it.
        every { viewModel.handleReturnToApp("pending", any(), any()) } answers {
            thirdArg<Intent?>()?.data = RETURN_URL.buildUpon().path("return").build()
            PayPalSavedPaymentMethodReturnResult.COMPLETED
        }
        startEdit()

        resumeAgain()

        verify { viewModel.handleReturnToApp("pending", any(), composeTestRule.activity.intent) }
        assertNull(composeTestRule.activity.intent.data)
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

    @Test
    fun `without an ActivityResultRegistry it reports a failure and doesn't create the view model`() {
        // Hosted by itself, not by composeTestRule, whose ComponentActivity always provides a registry. A plain
        // Activity isn't an ActivityResultRegistryOwner; the owners Compose needs come from another activity.
        val owners = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val reported = mutableListOf<PayPalResult>()
        var createdViewModels = 0
        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(owners)
            setViewTreeViewModelStoreOwner(owners)
            setViewTreeSavedStateRegistryOwner(owners)
            setContent {
                PayPalSavedPaymentMethodView(
                    payPalCheckoutRequest = PayPalCheckoutRequest(amount = "10.00", hasUserLocationConsent = true),
                    request = PayPalSavedPaymentMethodRequest("10.00", "USD"),
                    authorization = "client-token",
                    appLinkReturnUrl = Uri.parse("https://merchant.example.com"),
                    modifier = Modifier,
                    deepLinkFallbackUrlScheme = null,
                    style = PayPalSavedPaymentMethodViewStyle(),
                    paypalTokenizeCallback = { reported += it },
                    viewModelFactory = { _, _, _ ->
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                                createdViewModels++
                                return super.create(modelClass, extras)
                            }
                        }
                    }
                )
            }
        }

        activity.setContentView(composeView)
        shadowOf(Looper.getMainLooper()).idle()

        val error = (reported.single() as PayPalResult.Failure).error
        assertEquals(
            "PayPalSavedPaymentMethodView must be hosted in an activity that provides an ActivityResultRegistry.",
            error.message
        )
        assertEquals(0, createdViewModels)
    }

    // The keys the launcher's browser switch client registered its Activity result under.
    private fun registeredKeys(): List<String> {
        val keys = mutableListOf<String>()
        verify(atLeast = 0) {
            registry.register(
                capture(keys),
                any<ActivityResultContract<Any?, Any?>>(),
                any<ActivityResultCallback<Any?>>()
            )
        }
        return keys
    }

    // Taps the chip, whose edit returns [pending].
    private fun startEdit(pending: PayPalPendingRequest = PayPalPendingRequest.Started("pending")) {
        coEvery { viewModel.startEdit(any(), any(), any()) } returns pending
        composeTestRule.onNodeWithTag(FUNDING_INSTRUMENT_CHIP_TEST_TAG).performClick()
        composeTestRule.waitForIdle()
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
