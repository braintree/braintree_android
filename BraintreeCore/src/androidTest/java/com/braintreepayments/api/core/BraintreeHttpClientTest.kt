package com.braintreepayments.api.core

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization.Companion.fromString
import com.braintreepayments.api.sharedutils.AuthorizationException
import com.braintreepayments.api.testutils.Fixtures
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class BraintreeHttpClientTest {

    private lateinit var countDownLatch: CountDownLatch

    @Before
    fun setup() {
        countDownLatch = CountDownLatch(1)
    }

    @Throws(InterruptedException::class)
    @Test(timeout = 10000)
    fun requestSslCertificateSuccessfulInSandbox() {
        val authorization = fromString(Fixtures.TOKENIZATION_KEY)
        val braintreeHttpClient = BraintreeHttpClient()

        val path = "https://api.sandbox.braintreegateway.com/"
        braintreeHttpClient.get(path, null, authorization) { _, httpError ->
            // Make sure exception is due to authorization not SSL handshake
            assertTrue(httpError is AuthorizationException)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Throws(InterruptedException::class)
    @Test(timeout = 10000)
    fun requestSslCertificateSuccessfulInQA() {
        val authorization = fromString("development_testing_integration_merchant_id")
        val braintreeHttpClient = BraintreeHttpClient()

        val path = "https://gateway.qa.braintreepayments.com/"
        braintreeHttpClient.get(path, null, authorization) { _, httpError ->
            // Make sure http request to qa works to verify certificate pinning strategy
            assertNull(httpError)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Throws(InterruptedException::class)
    @Test(timeout = 10000)
    fun requestSslCertificateSuccessfulInProduction() {
        val authorization = fromString(Fixtures.PROD_TOKENIZATION_KEY)
        val braintreeHttpClient = BraintreeHttpClient()

        val path = "https://api.braintreegateway.com/"
        braintreeHttpClient.get(path, null, authorization) { _, httpError ->
            // Make sure exception is due to authorization not SSL handshake
            assertTrue(httpError is AuthorizationException)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Throws(InterruptedException::class)
    @Test(timeout = 10000)
    fun requestSslCertificateSuccessfulInGraphQL() {
        val authorization = fromString(Fixtures.TOKENIZATION_KEY)
        val braintreeHttpClient = BraintreeHttpClient()

        val path = "https://payments-qa.dev.braintree-api.com/"
        braintreeHttpClient.get(path, null, authorization) { _, httpError ->
            // Make sure exception is due to authorization not SSL handshake
            val message = "Expecting an AuthorizationException, but got $httpError"
            assertTrue(message, httpError is AuthorizationException)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }
}
