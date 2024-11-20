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

    private lateinit var sut: VenmoLauncher

    private val appSwitchUrl = Uri.parse("http://example.com")

    @Before
    fun setup() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        every { venmoRepository.venmoUrl } returns appSwitchUrl

        sut = VenmoLauncher(browserSwitchClient, venmoRepository, lazy { analyticsClient })
    }

    @Test
    fun `when launch is invoked, app switch started analytics event is sent`() {
        sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.APP_SWITCH_STARTED,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `launch starts browser switch and returns pending request`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options) } returns startedPendingRequest

        val pendingRequest =
            sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is VenmoPendingRequest.Started)
        assertEquals(
            startedPendingRequest.pendingRequest,
            (pendingRequest as VenmoPendingRequest.Started).pendingRequestString
        )
    }

    @Test
    fun `launch on error returns pending request failure`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        val exception = BrowserSwitchException("error")
        every { browserSwitchClient.start(eq(activity), eq(options)) } returns
            BrowserSwitchStartResult.Failure(exception)

        val pendingRequest =
            sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is VenmoPendingRequest.Failure)
        assertSame(exception, (pendingRequest as VenmoPendingRequest.Failure).error)
    }

    @Test
    @Throws(BrowserSwitchException::class)
    fun `launch when device cant perform browser switch returns pending request failure`() {
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
    fun `when handleReturnToApp is invoked, app switch started analytics event is sent`() {
        sut.handleReturnToApp(VenmoPendingRequest.Started(pendingRequestString), intent)

        verify {
            analyticsClient.sendEvent(
                eventName = VenmoAnalytics.HANDLE_RETURN_STARTED,
                analyticsEventParams = AnalyticsEventParams(appSwitchUrl = appSwitchUrl.toString())
            )
        }
    }

    @Test
    fun `handleReturnToApp when result exists returns result`() {
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
    fun `handleReturnToApp when result does not exist returns null`() {
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
    fun showVenmoInGooglePlayStore_opensVenmoAppStoreURL() {
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
}
