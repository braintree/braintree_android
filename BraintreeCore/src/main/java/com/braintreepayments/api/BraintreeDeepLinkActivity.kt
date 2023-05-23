package com.braintreepayments.api

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * This Activity is used by the SDK Activity to handle deep links on behalf of the merchant
 * It should not be used directly.
 */
class BraintreeDeepLinkActivity : AppCompatActivity() {
    private val browserSwitchClient = BrowserSwitchClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        browserSwitchClient.captureResult(this)
        finish()
    }
}
