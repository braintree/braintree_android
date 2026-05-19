package com.braintreepayments.api.venmo

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.BrowserSwitchOptions
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoPaymentAuthRequestParamsTest {

    @Test
    fun constructor_storesBrowserSwitchOptions() {
        val options = BrowserSwitchOptions()
        val params = VenmoPaymentAuthRequestParams(options)

        assertNotNull(params.browserSwitchOptions)
    }
}
