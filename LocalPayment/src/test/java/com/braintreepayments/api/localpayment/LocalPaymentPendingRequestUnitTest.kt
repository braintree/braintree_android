package com.braintreepayments.api.localpayment

import android.net.Uri
import com.braintreepayments.api.localpayment.LocalPaymentPendingRequest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalPaymentPendingRequestUnitTest {

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
        val pendingRequest = LocalPaymentPendingRequest.Started(browserSwitchPendingRequest)
        val storedRequest = pendingRequest.toJsonString()

        val sut = LocalPaymentPendingRequest.Started(storedRequest)
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
        val sut = LocalPaymentPendingRequest.Started("{}")
    }

    @Test
    fun `toJsonString returns BrowserSwitchRequest in JSON string format`() {
        val sut = LocalPaymentPendingRequest.Started(browserSwitchPendingRequest)
        val jsonString = sut.toJsonString()

        assertEquals(browserSwitchPendingRequest.toJsonString(), jsonString)
    }
}
