package com.braintreepayments.api.venmo

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.BrowserSwitchOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoSealedClassesTest {

    @Test
    fun venmoPendingRequestFailure_storesError() {
        val error = Exception("pending request failed")
        val failure = VenmoPendingRequest.Failure(error)

        assertEquals("pending request failed", failure.error.message)
    }

    @Test
    fun venmoPaymentAuthResultFailure_storesError() {
        val error = Exception("auth result failed")
        val failure = VenmoPaymentAuthResult.Failure(error)

        assertEquals("auth result failed", failure.error.message)
    }

    @Test
    fun venmoPaymentAuthRequestReadyToLaunch_storesParams() {
        val options = BrowserSwitchOptions()
        val params = VenmoPaymentAuthRequestParams(options)
        val readyToLaunch = VenmoPaymentAuthRequest.ReadyToLaunch(params)

        assertNotNull(readyToLaunch.requestParams)
    }
}
