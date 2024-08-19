package com.braintreepayments.api.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import junit.framework.Assert
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import javax.net.ssl.SSLException

@RunWith(AndroidJUnit4ClassRunner::class)
class BraintreeGraphQLClientTest {

    private lateinit var countDownLatch: CountDownLatch

    @Before
    fun setup() {
        countDownLatch = CountDownLatch(1)
    }

    @Test(timeout = 5000)
    @Throws(InterruptedException::class, JSONException::class)
    fun postRequestSslCertificateSuccessfulInSandbox() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val braintreeClient = BraintreeClient(context, Fixtures.TOKENIZATION_KEY)

        braintreeClient.sendGraphQLPOST(JSONObject("{}")) { _, httpError ->
            // Make sure SSL handshake is successful
            assertFalse(httpError is SSLException)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 5000)
    @Throws(InterruptedException::class, JSONException::class)
    fun postRequestSslCertificateSuccessfulInProduction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val braintreeClient = BraintreeClient(context, Fixtures.PROD_TOKENIZATION_KEY)

        braintreeClient.sendGraphQLPOST(JSONObject("{}")) { _, httpError ->
            // Make sure SSL handshake is successful
            assertFalse(httpError is SSLException)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }
}
