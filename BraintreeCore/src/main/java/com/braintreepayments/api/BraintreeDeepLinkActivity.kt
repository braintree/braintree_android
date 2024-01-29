package com.braintreepayments.api

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * This Activity is used by the SDK Activity to handle deep links on behalf of the merchant
 * It should not be used directly.
 */
// NEXT_MAJOR_VERSION : remove this class
class BraintreeDeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
