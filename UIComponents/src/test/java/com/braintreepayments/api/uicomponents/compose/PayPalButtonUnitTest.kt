package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import io.mockk.every
import io.mockk.EqMatcher
import io.mockk.MockKMatcherScope
import io.mockk.OfTypeMatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers what [PayPalButton] adds on top of [PayPalButtonFlow] (which has its own tests): analytics and delegation.
 * The flow constructs its own client and launcher, so those constructors are mocked.
 */
@RunWith(RobolectricTestRunner::class)
class PayPalButtonUnitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val repository: PendingRequestRepository = mockk(relaxed = true)
    private val payPalRequest: PayPalRequest = mockk(relaxed = true)
    private val results = mutableListOf<PayPalResult>()
    private val authCallbackSlot = slot<PayPalPaymentAuthCallback>()
    private val authorization = mutableStateOf("sandbox_tmxhyf7d_dcpspy2brwdjr3qn")
    private val callback = mutableStateOf(PayPalTokenizeCallback { results.add(it) })
    private val currentRepository = mutableStateOf(repository)
    private val requestAuthorizations = mutableListOf<String>()
    private val tokenizeAuthorizations = mutableListOf<String>()

    @Before
    fun setUp() {
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(analyticsClient)
        mockkConstructor(PayPalClient::class, PayPalLauncher::class)
        every { anyConstructed<PayPalLauncher>().launch(any(), any()) } returns PayPalPendingRequest.Started("pending")
        every { anyConstructed<PayPalLauncher>().restorePendingRequest(any()) } returns Unit
        every { anyConstructed<PayPalLauncher>().handleReturnToApp(any(), any()) } returns
            mockk<PayPalPaymentAuthResult.Success>(relaxed = true)
        listOf(authorization.value, "sandbox_9dbg82cq_dcpspy2brwdjr3qn").forEach { key ->
            every {
                clientFor(key).createPaymentAuthRequest(any(), any(), capture(authCallbackSlot))
            } answers { requestAuthorizations.add(key) }
            every { clientFor(key).tokenize(any(), any()) } answers {
                tokenizeAuthorizations.add(key)
                secondArg<PayPalTokenizeCallback>().onPayPalResult(PayPalResult.Cancel)
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun MockKMatcherScope.clientFor(key: String) = constructedWith<PayPalClient>(
        OfTypeMatcher<Context>(Context::class),
        EqMatcher(key),
        OfTypeMatcher<Uri>(Uri::class),
        EqMatcher("com.example.app")
    )

    private fun setContent(restorationTester: StateRestorationTester? = null) {
        val content = @Composable {
            PayPalButton(
                style = PayPalButtonColor.Blue,
                payPalRequest = payPalRequest,
                authorization = authorization.value,
                appLinkReturnUrl = Uri.parse("https://example.com/return"),
                deepLinkFallbackUrlScheme = "com.example.app",
                pendingRequestRepository = currentRepository.value,
                paypalTokenizeCallback = callback.value
            )
        }
        if (restorationTester == null) composeRule.setContent(content) else restorationTester.setContent(content)
        composeRule.waitForIdle()
    }

    private fun click() {
        composeRule.onRoot().performClick()
        composeRule.waitForIdle()
    }

    private fun pauseAndResume() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    @Test
    fun `sends presented event once with compose ui type`() {
        setContent()
        pauseAndResume()

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_PRESENTED,
                Authorization.fromString(authorization.value),
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `does not send selected event before the button is clicked`() {
        setContent()

        verify(exactly = 0) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED, any<Authorization>(), any<AnalyticsEventParams>()
            )
        }
    }

    @Test
    fun `click sends selected event and creates payment auth request with the request`() {
        setContent()

        click()

        verify(exactly = 1) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED,
                Authorization.fromString(authorization.value),
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
        verify { clientFor(authorization.value).createPaymentAuthRequest(any(), payPalRequest, any()) }
    }

    @Test
    fun `payment auth request Failure is forwarded to the tokenize callback`() {
        setContent()
        click()
        val error = Exception("auth request failed")
        val failure = mockk<PayPalPaymentAuthRequest.Failure>(relaxed = true)
        every { failure.error } returns error

        authCallbackSlot.captured.onPayPalPaymentAuthRequest(failure)
        composeRule.waitForIdle()

        assertEquals(error, (results.single() as PayPalResult.Failure).error)
    }

    @Test
    fun `no result is reported until the flow completes`() {
        setContent()
        click()

        assertTrue(results.isEmpty())
    }

    private fun launchFlow() {
        authCallbackSlot.captured.onPayPalPaymentAuthRequest(
            mockk<PayPalPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )
        composeRule.waitForIdle()
    }

    private fun prepareReturn() {
        coEvery { currentRepository.value.getPendingRequest() } returns "pending"
    }

    @Test
    fun `authorization change keeps in flight client and next click uses a new client and authorization`() {
        setContent()
        click()
        composeRule.runOnIdle { authorization.value = "sandbox_9dbg82cq_dcpspy2brwdjr3qn" }
        composeRule.waitForIdle()
        launchFlow()
        prepareReturn()

        pauseAndResume()
        assertEquals(requestAuthorizations.single(), tokenizeAuthorizations.single())
        click()

        assertEquals(
            listOf("sandbox_tmxhyf7d_dcpspy2brwdjr3qn", authorization.value),
            requestAuthorizations
        )
        verify {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED,
                Authorization.fromString(authorization.value),
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `updated callback receives delayed auth failure`() {
        setContent()
        click()
        val updatedResults = mutableListOf<PayPalResult>()
        composeRule.runOnIdle { callback.value = PayPalTokenizeCallback { updatedResults.add(it) } }
        composeRule.waitForIdle()

        val failure = mockk<PayPalPaymentAuthRequest.Failure>(relaxed = true)
        authCallbackSlot.captured.onPayPalPaymentAuthRequest(failure)
        composeRule.waitForIdle()

        assertTrue(results.isEmpty())
        assertTrue(updatedResults.single() is PayPalResult.Failure)
    }

    @Test
    fun `resume uses updated repository and callback`() {
        setContent()
        click()
        launchFlow()
        val updatedRepository = mockk<PendingRequestRepository>(relaxed = true)
        val updatedResults = mutableListOf<PayPalResult>()
        composeRule.runOnIdle {
            currentRepository.value = updatedRepository
            callback.value = PayPalTokenizeCallback { updatedResults.add(it) }
        }
        composeRule.waitForIdle()
        prepareReturn()

        pauseAndResume()

        assertTrue(results.joinToString { (it as? PayPalResult.Failure)?.error.toString() }, results.isEmpty())
        assertSame(PayPalResult.Cancel, updatedResults.single())
        coVerify { updatedRepository.getPendingRequest() }
        coVerify { updatedRepository.clearPendingRequest() }
    }

    @Test
    fun `restored flow resumes with current client when captured client is lost`() {
        val restorationTester = StateRestorationTester(composeRule)
        setContent(restorationTester)
        click()
        launchFlow()
        prepareReturn()

        composeRule.runOnIdle { authorization.value = "sandbox_9dbg82cq_dcpspy2brwdjr3qn" }
        composeRule.waitForIdle()
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertTrue(
            results.joinToString { (it as? PayPalResult.Failure)?.error.toString() },
            results.single() === PayPalResult.Cancel
        )
        assertEquals("sandbox_tmxhyf7d_dcpspy2brwdjr3qn", requestAuthorizations.single())
        assertEquals(authorization.value, tokenizeAuthorizations.single())
        verify { anyConstructed<PayPalLauncher>().restorePendingRequest("pending") }
        coVerify { repository.clearPendingRequest() }
    }
}
