package com.braintreepayments.api.core

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization.Companion.fromString
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4ClassRunner::class)
class BraintreeHttpClientTest {

    @Test
    fun requestSslCertificateSuccessfulInSandbox() = runTest {
        val authorization = fromString(Fixtures.TOKENIZATION_KEY)
        val braintreeHttpClient = BraintreeHttpClient()

        val path = "https://api.sandbox.braintreegateway.com/"
        try {
            braintreeHttpClient.get(path, null, authorization)
            fail("Must throw an exception")
        } catch (e: IOException) {
            // Make sure exception is due to authorization not SSL handshake
            assertTrue(e.message?.contains("code=403") == true)
        }
    }

    @Test
    fun requestSslCertificateSuccessfulInQA() = runTest {
        val authorization = fromString("development_testing_integration_merchant_id")
        val braintreeHttpClient = BraintreeHttpClient()

        val path = "https://gateway.qa.braintreepayments.com/"
        try {
            val response = braintreeHttpClient.get(path, null, authorization)
            assertNotNull(response)
        } catch (e: IOException) {
            fail("Exception must not be thrown. Exception: ${e.message}")
        }
    }

    @Test
    fun requestSslCertificateSuccessfulInProduction() = runTest {
        val authorization = fromString(Fixtures.PROD_TOKENIZATION_KEY)
        val braintreeHttpClient = BraintreeHttpClient()

        val path = "https://api.braintreegateway.com/"
        try {
            braintreeHttpClient.get(path, null, authorization)
            fail("Must throw an exception")
        } catch (e: IOException) {
            // Make sure exception is due to authorization not SSL handshake
            assertTrue(e.message?.contains("code=403") == true)
        }
    }
}
