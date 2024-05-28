package com.braintreepayments.api.paypal

import android.net.Uri
import com.braintreepayments.api.BrowserSwitchPendingRequest
import com.braintreepayments.api.BrowserSwitchRequest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalPendingRequestUnitTest {

    private val browserSwitchRequest = BrowserSwitchRequest(
        1,
        Uri.parse("http://"),
        JSONObject().put("test_key", "test_value"),
        "return-url-scheme",
        false
    )

    private val browserSwitchPendingRequest =
        BrowserSwitchPendingRequest.Started(browserSwitchRequest)

    @Test
    fun `started constructor with string parameter creates BrowserSwitchRequest`() {
        val pendingRequest = PayPalPendingRequest.Started(browserSwitchPendingRequest)
        val storedRequest = pendingRequest.toJsonString()

        val sut = PayPalPendingRequest.Started(storedRequest)
        assertEquals(1, sut.request.browserSwitchRequest.requestCode)
        assertEquals(
            "test_value",
            sut.request.browserSwitchRequest.metadata.getString("test_key")
        )
        assertEquals(
            Uri.parse("http://"),
            sut.request.browserSwitchRequest.url
        )
        assertEquals(
            false,
            sut.request.browserSwitchRequest.shouldNotifyCancellation
        )
        assertEquals(
            "return-url-scheme",
            sut.request.browserSwitchRequest.returnUrlScheme
        )
    }

    @Test(expected = JSONException::class)
    fun `started constructor with invalid input string throws JSONException`() {
        val sut = PayPalPendingRequest.Started("{}")
    }

    @Test
    fun `toJsonString returns BrowserSwitchRequest in JSON string format`() {
        val sut = PayPalPendingRequest.Started(browserSwitchPendingRequest)
        val jsonString = sut.toJsonString()

        assertEquals(browserSwitchPendingRequest.toJsonString(), jsonString)
    }
}
