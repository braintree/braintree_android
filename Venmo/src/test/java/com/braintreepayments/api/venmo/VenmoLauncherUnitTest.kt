package com.braintreepayments.api.venmo

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchPendingRequest
import com.braintreepayments.api.BrowserSwitchRequest
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchResultInfo
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert
import org.json.JSONException
import org.json.JSONObject
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
    private val activity: ComponentActivity = mockk(relaxed = true)
    private val paymentAuthRequestParams: VenmoPaymentAuthRequestParams = mockk(relaxed = true)
    private val intent: Intent = mockk(relaxed = true)
    private val options: BrowserSwitchOptions = mockk(relaxed = true)
    private val browserSwitchRequest = BrowserSwitchRequest(
        1,
        Uri.parse("http://"),
        JSONObject().put("test_key", "test_value"),
        "return-url-scheme",
        false
    )
    private lateinit var sut: VenmoLauncher

    @Before
    fun setup() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        sut = VenmoLauncher(browserSwitchClient)
    }

    @Test
    fun `launch starts browser switch and returns pending request`() {
        val startedPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        every { browserSwitchClient.start(activity, options) } returns startedPendingRequest

        val pendingRequest =
            sut.launch(activity, VenmoPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is VenmoPendingRequest.Started)
        assertEquals(
            browserSwitchRequest,
            (pendingRequest as VenmoPendingRequest.Started).request.browserSwitchRequest
        )
    }

    @Test
    fun `launch on error returns pending request failure`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        val exception = BrowserSwitchException("error")
        every { browserSwitchClient.start(eq(activity), eq(options)) } returns
                BrowserSwitchPendingRequest.Failure(exception)

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
    @Throws(JSONException::class)
    fun `handleReturnToAppFromBrowser when result exists returns result`() {
        val result: BrowserSwitchResultInfo = mockk(relaxed = true)
        val browserSwitchPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        every {
            browserSwitchClient.parseResult(
                browserSwitchPendingRequest,
                intent
            )
        } returns BrowserSwitchResult.Success(result)

        val paymentAuthResult = sut.handleReturnToApp(
            VenmoPendingRequest.Started(browserSwitchPendingRequest), intent
        )

        assertTrue(paymentAuthResult is VenmoPaymentAuthResult.Success)
        assertSame(
            result,
            (paymentAuthResult as VenmoPaymentAuthResult.Success).paymentAuthInfo.browserSwitchResultInfo
        )
    }

    @Test
    @Throws(JSONException::class)
    fun `handleReturnToAppFromBrowser when result does not exist returns null`() {
        val browserSwitchPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        every {
            browserSwitchClient.parseResult(
                browserSwitchPendingRequest,
                intent
            )
        } returns BrowserSwitchResult.NoResult

        val paymentAuthResult = sut.handleReturnToApp(
            VenmoPendingRequest.Started(browserSwitchPendingRequest), intent
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
