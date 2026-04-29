package com.braintreepayments.api.venmo

import android.util.Base64
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.core.BraintreeRequestCodes
import org.junit.Assert.assertNotNull
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoLauncherTest {

    private val returnUrlScheme = "com.braintreepayments.api.venmo.test"

    @Test
    fun restorePendingRequest_withValidPendingRequestString_doesNotCrash() {
        val launcher = VenmoLauncher()
        val pendingRequestString = buildPendingRequestString()

        try {
            launcher.restorePendingRequest(pendingRequestString)
        } catch (e: BrowserSwitchException) {
            assertNotNull(e)
        }
    }

    private fun buildPendingRequestString(): String {
        val approvalUrl = "https://venmo.com/go/checkout?resource_id=fake-resource-id"
        val pendingRequestJson = JSONObject()
            .put("requestCode", BraintreeRequestCodes.VENMO.code)
            .put("url", approvalUrl)
            .put("returnUrlScheme", returnUrlScheme)
            .put("metadata", JSONObject())
        return Base64.encodeToString(
            pendingRequestJson.toString().toByteArray(Charsets.UTF_8),
            Base64.DEFAULT
        )
    }
}
