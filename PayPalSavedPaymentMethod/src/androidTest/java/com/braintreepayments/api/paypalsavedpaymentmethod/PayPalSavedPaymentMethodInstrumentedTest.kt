package com.braintreepayments.api.paypalsavedpaymentmethod

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
// TODO: placeholder scaffold test - replace with real instrumented tests for PayPalSavedPaymentMethod
@RunWith(AndroidJUnit4::class)
class PayPalSavedPaymentMethodInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.braintreepayments.api.paypalsavedpaymentmethod.test", appContext.packageName)
    }
}
