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
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import com.braintreepayments.api.venmo.VenmoClient
import com.braintreepayments.api.venmo.VenmoLauncher
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequestCallback
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.EqMatcher
import io.mockk.MockKMatcherScope
import io.mockk.OfTypeMatcher
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

@RunWith(RobolectricTestRunner::class)
class VenmoButtonAuthorizationUnitTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsClient = mockk<AnalyticsClient>(relaxed = true)
    private val repository = mockk<PendingRequestRepository>(relaxed = true)
    private val request = mockk<VenmoRequest>(relaxed = true)
    private val results = mutableListOf<VenmoResult>()
    private val authorization = mutableStateOf("sandbox_tmxhyf7d_dcpspy2brwdjr3qn")
    private val callback = mutableStateOf(VenmoTokenizeCallback { results.add(it) })
    private val currentRepository = mutableStateOf(repository)
    private val authCallback = slot<VenmoPaymentAuthRequestCallback>()
    private val requestAuthorizations = mutableListOf<String>()
    private val tokenizeAuthorizations = mutableListOf<String>()

    @Before
    fun setUp() {
        mockkObject(AnalyticsClient.Companion)
        every { AnalyticsClient.lazyInstance } returns lazyOf(analyticsClient)
        mockkConstructor(VenmoClient::class, VenmoLauncher::class)
        every { anyConstructed<VenmoLauncher>().launch(any(), any()) } returns VenmoPendingRequest.Started("pending")
        every { anyConstructed<VenmoLauncher>().restorePendingRequest(any()) } returns Unit
        every { anyConstructed<VenmoLauncher>().handleReturnToApp(any(), any()) } returns
            mockk<VenmoPaymentAuthResult.Success>(relaxed = true)
        listOf(authorization.value, "sandbox_9dbg82cq_dcpspy2brwdjr3qn").forEach { key ->
            every {
                clientFor(key).createPaymentAuthRequest(any(), any(), capture(authCallback))
            } answers { requestAuthorizations.add(key) }
            every { clientFor(key).tokenize(any(), any()) } answers {
                tokenizeAuthorizations.add(key)
                secondArg<VenmoTokenizeCallback>().onVenmoResult(VenmoResult.Cancel)
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun MockKMatcherScope.clientFor(key: String) = constructedWith<VenmoClient>(
        OfTypeMatcher<Context>(Context::class),
        EqMatcher(key),
        OfTypeMatcher<Uri>(Uri::class),
        EqMatcher("com.example.app")
    )

    private fun setContent(restorationTester: StateRestorationTester? = null) {
        val content = @Composable {
            VenmoButton(
                style = VenmoButtonColor.Blue,
                venmoRequest = request,
                authorization = authorization.value,
                appLinkReturnUrl = Uri.parse("https://example.com/return"),
                deepLinkFallbackUrlScheme = "com.example.app",
                pendingRequestRepository = currentRepository.value,
                venmoTokenizeCallback = callback.value
            )
        }
        if (restorationTester == null) composeRule.setContent(content) else restorationTester.setContent(content)
        composeRule.waitForIdle()
    }

    private fun click() {
        composeRule.onRoot().performClick()
        composeRule.waitForIdle()
    }

    private fun launchFlow() {
        authCallback.captured.onVenmoPaymentAuthRequest(
            mockk<VenmoPaymentAuthRequest.ReadyToLaunch>(relaxed = true)
        )
        composeRule.waitForIdle()
    }

    private fun prepareReturn() {
        coEvery { currentRepository.value.getPendingRequest() } returns "pending"
    }

    private fun pauseAndResume() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
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
                UIComponentsAnalytics.VENMO_BUTTON_PRESENTED,
                Authorization.fromString("sandbox_tmxhyf7d_dcpspy2brwdjr3qn"),
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
            analyticsClient.sendEvent(
                UIComponentsAnalytics.VENMO_BUTTON_SELECTED,
                Authorization.fromString(authorization.value),
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
        }
    }

    @Test
    fun `updated callback receives delayed auth failure`() {
        setContent()
        click()
        val updatedResults = mutableListOf<VenmoResult>()
        composeRule.runOnIdle { callback.value = VenmoTokenizeCallback { updatedResults.add(it) } }
        composeRule.waitForIdle()

        val failure = mockk<VenmoPaymentAuthRequest.Failure>(relaxed = true)
        authCallback.captured.onVenmoPaymentAuthRequest(failure)
        composeRule.waitForIdle()

        assertTrue(results.isEmpty())
        assertTrue(updatedResults.single() is VenmoResult.Failure)
    }

    @Test
    fun `resume uses updated repository and callback`() {
        setContent()
        click()
        launchFlow()
        val updatedRepository = mockk<PendingRequestRepository>(relaxed = true)
        val updatedResults = mutableListOf<VenmoResult>()
        composeRule.runOnIdle {
            currentRepository.value = updatedRepository
            callback.value = VenmoTokenizeCallback { updatedResults.add(it) }
        }
        composeRule.waitForIdle()
        prepareReturn()

        pauseAndResume()

        assertTrue(results.joinToString { (it as? VenmoResult.Failure)?.error.toString() }, results.isEmpty())
        assertSame(VenmoResult.Cancel, updatedResults.single())
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
            results.joinToString { (it as? VenmoResult.Failure)?.error.toString() },
            results.single() === VenmoResult.Cancel
        )
        assertEquals("sandbox_tmxhyf7d_dcpspy2brwdjr3qn", requestAuthorizations.single())
        assertEquals(authorization.value, tokenizeAuthorizations.single())
        verify { anyConstructed<VenmoLauncher>().restorePendingRequest("pending") }
        coVerify { repository.clearPendingRequest() }
    }
}
