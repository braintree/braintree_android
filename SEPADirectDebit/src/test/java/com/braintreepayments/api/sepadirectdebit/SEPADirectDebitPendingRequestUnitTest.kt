package com.braintreepayments.api.sepadirectdebit

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
class SEPADirectDebitPendingRequestUnitTest {

    private val browserSwitchRequest = BrowserSwitchRequest(
        1,
        Uri.parse("http://"),
        JSONObject().put("test_key", "test_value"),
        "return-url-scheme",
        Uri.parse("https://example.com"),
        false
    )

    private val browserSwitchPendingRequest =
        BrowserSwitchPendingRequest.Started(browserSwitchRequest)

    @Test
    fun startedConstructor_fromString_createsBrowserSwitchRequest() {
        val pendingRequest = SEPADirectDebitPendingRequest.Started(browserSwitchPendingRequest)
        val storedRequest = pendingRequest.toJsonString()

        val sut = SEPADirectDebitPendingRequest.Started(storedRequest)
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
    fun startedConstructor_fromString_whenInvalidString_throwsJSONException() {
        val sut = SEPADirectDebitPendingRequest.Started("{}")
    }

    @Test
    fun toJsonString_returnsJsonBrowserSwitchRequest() {
        val sut = SEPADirectDebitPendingRequest.Started(browserSwitchPendingRequest)
        val jsonString = sut.toJsonString()

        assertEquals(browserSwitchPendingRequest.toJsonString(), jsonString)
    }
}
