package com.braintreepayments.api.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.paypal.PayPalAccount
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.paypal.PayPalPaymentIntent
import com.braintreepayments.api.testutils.Assertions
import com.braintreepayments.api.testutils.Fixtures
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class ApiClientTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test(timeout = 10000)
    @Ignore("It isn't clear what this test does or how it works, but we removed a lot of the PayPal OTC logic when refactoring for v4.")
    @Throws(
        InterruptedException::class, JSONException::class
    )
    fun tokenize_tokenizesAPayPalAccountWithATokenizationKey() {
        val latch = CountDownLatch(1)
        val braintreeClient = BraintreeClient(context, Fixtures.TOKENIZATION_KEY)
        val apiClient = ApiClient(braintreeClient)

        val urlResponseData = JSONObject(Fixtures.PAYPAL_OTC_RESPONSE)
        val paypalAccount = PayPalAccount(
            clientMetadataId = "fake-client-metadata-id",
            urlResponseData = urlResponseData,
            intent = PayPalPaymentIntent.AUTHORIZE,
            merchantAccountId = null,
            paymentType = "single-payment"
        )

        apiClient.tokenizeREST(paypalAccount) { tokenizationResponse: JSONObject?, exception: Exception? ->
            if (exception != null) {
                fail(exception.message)
            }
            try {
                val payPalAccountNonce: PayPalAccountNonce =
                    PayPalAccountNonce.fromJSON(tokenizationResponse!!)
                Assertions.assertIsANonce(payPalAccountNonce.string)
                latch.countDown()
            } catch (e: JSONException) {
                fail("This should not fail")
            }
        }

        latch.await()
    }
}
