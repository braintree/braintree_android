package com.braintreepayments.api.venmo

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.MerchantRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VenmoLauncherUnitTest {
    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val venmoRepository: VenmoRepository = mockk(relaxed = true)
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val activity: ComponentActivity = mockk(relaxed = true)
    private val paymentAuthRequestParams: VenmoPaymentAuthRequestParams = mockk(relaxed = true)
    private val intent: Intent = mockk(relaxed = true)
    private val pendingRequestString = "pending_request_string"
    private val options: BrowserSwitchOptions = mockk(relaxed = true)

    private val authorization: Authorization = mockk()
    private val merchantRepository = MerchantRepository().also { it.authorization = authorization }

    private lateinit var sut: VenmoLauncher

    private val appSwitchUrl = Uri.parse("http://example.com")

    @Before
    fun setup() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        every { paymentAuthRequestParams.authorization } returns authorization
        every { venmoRepository.venmoUrl } returns appSwitchUrl

        sut = VenmoLauncher(browserSwitchClient, venmoRepository, lazy { analyticsClient }, merchantRepository)
    }

    @Test
    fun `when launch is invoked, app switch started analytics event is sent`() {
        sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.APP_SWITCH_STARTED,
                authorization = authorization,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `when launch succeeds, it returns a started pending request`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, true) } returns startedPendingRequest

        val pendingRequest = sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is VenmoPendingRequest.Started)
        assertEquals(
            startedPendingRequest.pendingRequest,
            (pendingRequest as VenmoPendingRequest.Started).pendingRequestString
        )
    }

    @Test
    fun `when launch is called and Started is returned, app switch succeeded analytics event is sent`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, true) } returns startedPendingRequest

        sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.APP_SWITCH_SUCCEEDED,
                authorization = authorization,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `when browser switch start fails, launch returns a pending request failure`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        val exception = BrowserSwitchException("error")
        every { browserSwitchClient.start(eq(activity), eq(options), eq(true)) } returns
            BrowserSwitchStartResult.Failure(exception)

        val pendingRequest =
            sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is VenmoPendingRequest.Failure)
        assertSame(exception, (pendingRequest as VenmoPendingRequest.Failure).error)
    }

    @Test
    fun `when launch is called and Failure is returned, app switch failed analytics event is sent`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        every { browserSwitchClient.start(eq(activity), eq(options), eq(true)) } returns
            BrowserSwitchStartResult.Failure(BrowserSwitchException("error"))

        sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.APP_SWITCH_FAILED,
                authorization = authorization,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `when the device cannot perform a browser switch, launch returns a pending request failure`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        val exception = BrowserSwitchException("browser switch error")
        every {
            browserSwitchClient.assertCanPerformBrowserSwitch(
                eq(activity),
                eq(options)
            )
        } throws exception

        val pendingRequest =
            sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is VenmoPendingRequest.Failure)
        assertEquals(
            "AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/" +
                "android/v4#browser-switch-setup " +
                "for the correct configuration: browser switch error",
            (pendingRequest as VenmoPendingRequest.Failure).error.message
        )
    }

    @Test
    @Throws(BrowserSwitchException::class)
    fun `when launch is called with a manifest error, app switch failed analytics event is sent`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        every { browserSwitchClient.assertCanPerformBrowserSwitch(eq(activity), eq(options)) } throws
            BrowserSwitchException("browser switch error")

        sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.APP_SWITCH_FAILED,
                authorization = authorization,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `when handleReturnToApp is invoked, app switch started analytics event is sent`() {
        sut.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.HANDLE_RETURN_STARTED,
                authorization = authorization,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `when a browser switch result exists, handleReturnToApp returns a success result`() {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
        every {
            browserSwitchClient.completeRequest(
                intent,
                pendingRequestString
            )
        } returns browserSwitchFinalResult

        val paymentAuthResult = sut.handleReturnToApp(
            VenmoPendingRequest.Started(pendingRequestString), intent
        )

        assertTrue(paymentAuthResult is VenmoPaymentAuthResult.Success)
        assertSame(
            browserSwitchFinalResult,
            (paymentAuthResult as VenmoPaymentAuthResult.Success).browserSwitchSuccess
        )
    }

    @Test
    fun `when handleReturnToApp is called with Success, handle return succeeded analytics event is sent`() {
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            mockk<BrowserSwitchFinalResult.Success>()

        sut.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.HANDLE_RETURN_SUCCEEDED,
                authorization = authorization,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `when no browser switch result exists, handleReturnToApp returns NoResult`() {
        every {
            browserSwitchClient.completeRequest(
                intent,
                pendingRequestString
            )
        } returns BrowserSwitchFinalResult.NoResult

        val paymentAuthResult = sut.handleReturnToApp(
            VenmoPendingRequest.Started(pendingRequestString),
            intent
        )

        assertTrue(paymentAuthResult is VenmoPaymentAuthResult.NoResult)
    }

    @Test
    fun `when handleReturnToApp is called without a result, handle return no result analytics event is sent`() {
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            BrowserSwitchFinalResult.NoResult

        sut.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.HANDLE_RETURN_NO_RESULT,
                authorization = authorization,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `when handleReturnToApp is called with a failure, handle return failed analytics event is sent`() {
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            mockk<BrowserSwitchFinalResult.Failure>(relaxed = true)

        sut.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.HANDLE_RETURN_FAILED,
                authorization = authorization,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `showVenmoInGooglePlayStore starts an activity for the venmo play store url`() {
        val activity = Mockito.mock(
            ComponentActivity::class.java
        )

        sut.showVenmoInGooglePlayStore(activity)
        val captor = ArgumentCaptor.forClass(Intent::class.java)
        Mockito.verify(activity).startActivity(captor.capture())
        Assert.assertEquals(
            captor.value.data.toString(),
            "https://play.google.com/store/apps/details?id=com.venmo"
        )
    }

    private fun createSut(
        repository: MerchantRepository,
        lazyClient: Lazy<AnalyticsClient> = lazy { analyticsClient }
    ) = VenmoLauncher(browserSwitchClient, venmoRepository, lazyClient, repository)

    private fun repositoryWith(auth: Authorization) = MerchantRepository().also { it.authorization = auth }

    private fun launchStarted(launcher: VenmoLauncher, pendingRequest: String) {
        every { browserSwitchClient.start(activity, options, true) } returns
            BrowserSwitchStartResult.Started(pendingRequest)
        launcher.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))
    }

    @Test
    fun `when no authorization is available, launch and handleReturnToApp skip analytics and return results`() {
        val failIfEvaluated: Lazy<AnalyticsClient> = lazy { throw AssertionError("evaluated") }
        every { paymentAuthRequestParams.authorization } returns null
        val launcher = createSut(MerchantRepository(), failIfEvaluated)
        every { browserSwitchClient.start(activity, options, true) } returns
            BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            BrowserSwitchFinalResult.NoResult

        val pendingRequest =
            launcher.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))
        val result = launcher.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        assertTrue(pendingRequest is VenmoPendingRequest.Started)
        assertTrue(result is VenmoPaymentAuthResult.NoResult)
    }

    @Test
    fun `launch events carry the request params authorization`() {
        val other: Authorization = mockk()
        val launcher = createSut(repositoryWith(other))

        launchStarted(launcher, pendingRequestString)

        verify { analyticsClient.sendEvent(VenmoAnalytics.APP_SWITCH_STARTED, authorization, any()) }
        verify { analyticsClient.sendEvent(VenmoAnalytics.APP_SWITCH_SUCCEEDED, authorization, any()) }
        verify(exactly = 0) { analyticsClient.sendEvent(any(), other, any<AnalyticsEventParams>()) }
    }

    @Test
    fun `handleReturnToApp events use the authorization of the launch that started the pending request`() {
        val other: Authorization = mockk()
        val launcher = createSut(repositoryWith(other))
        launchStarted(launcher, pendingRequestString)
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            BrowserSwitchFinalResult.NoResult

        launcher.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_STARTED, authorization, any()) }
        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_NO_RESULT, authorization, any()) }
        verify(exactly = 0) { analyticsClient.sendEvent(any(), other, any<AnalyticsEventParams>()) }
    }

    @Test
    fun `handleReturnToApp with an unknown pending request falls back to the merchant repository authorization`() {
        val newest: Authorization = mockk()
        val launcher = createSut(repositoryWith(newest))
        launchStarted(launcher, pendingRequestString)
        every { browserSwitchClient.completeRequest(intent, "other_request") } returns
            BrowserSwitchFinalResult.NoResult

        launcher.handleReturnToApp(VenmoPendingRequest.Started("other_request"), intent)

        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_STARTED, newest, any()) }
        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_NO_RESULT, newest, any()) }
    }

    @Test
    fun `a failed launch does not remember the request`() {
        val newest: Authorization = mockk()
        val launcher = createSut(repositoryWith(newest))
        every { browserSwitchClient.start(activity, options, true) } returns
            BrowserSwitchStartResult.Failure(BrowserSwitchException("error"))
        launcher.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            BrowserSwitchFinalResult.NoResult

        launcher.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_STARTED, newest, any()) }
    }

    @Test
    fun `a later launch replaces the remembered request only when it starts successfully`() {
        val second: Authorization = mockk()
        val newest: Authorization = mockk()
        val launcher = createSut(repositoryWith(newest))
        launchStarted(launcher, "first_request")

        every { paymentAuthRequestParams.authorization } returns second
        every { browserSwitchClient.start(activity, options, true) } returns
            BrowserSwitchStartResult.Failure(BrowserSwitchException("error"))
        launcher.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))
        every { browserSwitchClient.completeRequest(intent, any()) } returns BrowserSwitchFinalResult.NoResult
        launcher.handleReturnToApp(VenmoPendingRequest.Started("first_request"), intent)
        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_STARTED, authorization, any()) }

        launchStarted(launcher, "second_request")
        launcher.handleReturnToApp(VenmoPendingRequest.Started("first_request"), intent)
        launcher.handleReturnToApp(VenmoPendingRequest.Started("second_request"), intent)

        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_STARTED, second, any()) }
        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_STARTED, newest, any()) }
    }

    @Test
    fun `handleReturnToApp clears the remembered request after a success result`() {
        val newest: Authorization = mockk()
        val launcher = createSut(repositoryWith(newest))
        launchStarted(launcher, pendingRequestString)
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            mockk<BrowserSwitchFinalResult.Success>()

        launcher.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)
        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_SUCCEEDED, authorization, any()) }

        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            BrowserSwitchFinalResult.NoResult
        launcher.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_NO_RESULT, newest, any()) }
        verify(exactly = 0) { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_NO_RESULT, authorization, any()) }
    }

    @Test
    fun `handleReturnToApp clears the remembered request after a failure result`() {
        val newest: Authorization = mockk()
        val launcher = createSut(repositoryWith(newest))
        launchStarted(launcher, pendingRequestString)
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            mockk<BrowserSwitchFinalResult.Failure> { every { error } returns BrowserSwitchException("error") }

        launcher.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)
        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_FAILED, authorization, any()) }

        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            BrowserSwitchFinalResult.NoResult
        launcher.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_NO_RESULT, newest, any()) }
        verify(exactly = 0) { analyticsClient.sendEvent(VenmoAnalytics.HANDLE_RETURN_NO_RESULT, authorization, any()) }
    }
}
