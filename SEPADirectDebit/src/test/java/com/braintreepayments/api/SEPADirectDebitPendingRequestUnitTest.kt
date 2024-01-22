package com.braintreepayments.api

import android.net.Uri
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SEPADirectDebitPendingRequestUnitTest {

    private val browserSwitchRequest = BrowserSwitchRequest(
        1,
        Uri.parse("http://"),
        JSONObject().put("test_key", "test_value"),
        "return-url-scheme",
        false
    )

    private val browserSwitchPendingRequest =
        BrowserSwitchPendingRequest.Started(browserSwitchRequest)

    private val payPalBrowserSwitchRequest = SEPADirectDebitBrowserSwitchRequest(browserSwitchPendingRequest)

    @Test
    fun startedConstructor_fromString_createsBrowserSwitchRequest() {
        val pendingRequest = SEPADirectDebitPendingRequest.Started(payPalBrowserSwitchRequest)
        val storedRequest = pendingRequest.toJsonString()

        val sut = SEPADirectDebitPendingRequest.Started(storedRequest)
        assertEquals(1, sut.request.getBrowserSwitchPendingRequest().browserSwitchRequest.requestCode)
        assertEquals(
            "test_value",
            sut.request.getBrowserSwitchPendingRequest().browserSwitchRequest.metadata.getString("test_key")
        )
        assertEquals(
            Uri.parse("http://"),
            sut.request.getBrowserSwitchPendingRequest().browserSwitchRequest.url
        )
        assertEquals(
            false,
            sut.request.getBrowserSwitchPendingRequest().browserSwitchRequest.shouldNotifyCancellation
        )
        assertEquals(
            "return-url-scheme",
            sut.request.getBrowserSwitchPendingRequest().browserSwitchRequest.returnUrlScheme
        )
    }

    @Test(expected = JSONException::class)
    fun startedConstructor_fromString_whenInvalidString_throwsJSONException() {
        val sut = SEPADirectDebitPendingRequest.Started("{}")
    }

    @Test
    fun toJsonString_returnsJsonBrowserSwitchRequest() {
        val sut = SEPADirectDebitPendingRequest.Started(payPalBrowserSwitchRequest)
        val jsonString = sut.toJsonString()

        assertEquals(browserSwitchPendingRequest.toJsonString(), jsonString)
    }
}
