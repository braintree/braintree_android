package com.braintreepayments.api.venmo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoClientTest {

    private lateinit var context: Context
    private lateinit var countDownLatch: CountDownLatch

    private val appLinkReturnUrl = Uri.parse("https://example.com/braintree")
    private val returnUrlScheme = "com.braintreepayments.api.venmo.test"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        countDownLatch = CountDownLatch(1)
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_whenVenmoDisabled_returnsFailure() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)

        val sut = VenmoClient(
            context = context,
            authorization = Fixtures.TOKENIZATION_KEY,
            appLinkReturnUrl = appLinkReturnUrl
        )

        val request = VenmoRequest(paymentMethodUsage = VenmoPaymentMethodUsage.MULTI_USE)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is VenmoPaymentAuthRequest.Failure)
            val failure = result as VenmoPaymentAuthRequest.Failure
            assertTrue(failure.error.message?.contains("Venmo is not enabled") == true)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withInvalidAuthorization_returnsFailure() {
        val sut = VenmoClient(
            context = context,
            authorization = "sandbox_invalid_key_xxxxxxxx",
            appLinkReturnUrl = appLinkReturnUrl
        )

        val request = VenmoRequest(paymentMethodUsage = VenmoPaymentMethodUsage.MULTI_USE)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is VenmoPaymentAuthRequest.Failure)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_whenCollectingShippingAddress_andECDDisabled_returnsFailure() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)

        val sut = VenmoClient(
            context = context,
            authorization = Fixtures.TOKENIZATION_KEY,
            appLinkReturnUrl = appLinkReturnUrl
        )

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.MULTI_USE,
            collectCustomerShippingAddress = true
        )

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is VenmoPaymentAuthRequest.Failure)
            val failure = result as VenmoPaymentAuthRequest.Failure
            assertTrue(failure.error.message?.contains("ECD is disabled") == true)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_whenCollectingBillingAddress_andECDDisabled_returnsFailure() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)

        val sut = VenmoClient(
            context = context,
            authorization = Fixtures.TOKENIZATION_KEY,
            appLinkReturnUrl = appLinkReturnUrl
        )

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.MULTI_USE,
            collectCustomerBillingAddress = true
        )

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is VenmoPaymentAuthRequest.Failure)
            val failure = result as VenmoPaymentAuthRequest.Failure
            assertTrue(failure.error.message?.contains("ECD is disabled") == true)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_whenVenmoEnabled_andNetworkFails_returnsFailure() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)

        val sut = VenmoClient(
            context = context,
            authorization = Fixtures.TOKENIZATION_KEY,
            appLinkReturnUrl = appLinkReturnUrl
        )

        val request = VenmoRequest(paymentMethodUsage = VenmoPaymentMethodUsage.MULTI_USE)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is VenmoPaymentAuthRequest.Failure)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun tokenize_whenSuccessUrlUsesAmpersandSeparator_returnsSuccess() {
        val sut = createVenmoClient()
        val returnPath = "x-callback-url/vzero/auth/venmo/success&payment_method_nonce=fake-venmo-nonce&username=venmojoe"
        val paymentAuthResult = simulateVenmoReturn(returnPath)
        assertTrue(paymentAuthResult is VenmoPaymentAuthResult.Success)

        var tokenizeResult: VenmoResult? = null
        sut.tokenize(paymentAuthResult as VenmoPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            countDownLatch.countDown()
        }
        countDownLatch.await()

        assertTrue(tokenizeResult is VenmoResult.Success)
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun tokenize_whenCancelUrlReceived_returnsCancelResult() {
        val sut = createVenmoClient()
        val paymentAuthResult = simulateVenmoReturn("x-callback-url/vzero/auth/venmo/cancel")
        assertTrue(paymentAuthResult is VenmoPaymentAuthResult.Success)

        sut.tokenize(paymentAuthResult as VenmoPaymentAuthResult.Success) { result ->
            assertTrue(result is VenmoResult.Cancel)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun tokenize_whenErrorUrlReceived_returnsFailure() {
        val sut = createVenmoClient()
        val paymentAuthResult = simulateVenmoReturn("x-callback-url/vzero/auth/venmo/error")
        assertTrue(paymentAuthResult is VenmoPaymentAuthResult.Success)

        sut.tokenize(paymentAuthResult as VenmoPaymentAuthResult.Success) { result ->
            assertTrue(result is VenmoResult.Failure)
            assertNotNull((result as VenmoResult.Failure).error)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun tokenize_withPaymentMethodNonceAndUsername_returnsSuccess() {
        val sut = createVenmoClient()
        val returnPath = "x-callback-url/vzero/auth/venmo/success" +
                "?payment_method_nonce=fake-venmo-nonce&username=venmojoe"
        val paymentAuthResult = simulateVenmoReturn(returnPath)
        assertTrue(paymentAuthResult is VenmoPaymentAuthResult.Success)

        var tokenizeResult: VenmoResult? = null
        sut.tokenize(paymentAuthResult as VenmoPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            countDownLatch.countDown()
        }
        countDownLatch.await()

        assertTrue(tokenizeResult is VenmoResult.Success)
        val success = tokenizeResult as VenmoResult.Success
        assertNotNull(success.nonce)
        assertTrue(success.nonce.username == "venmojoe")
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun tokenize_whenSuccessUrlWithNeitherNonceNorContextId_returnsFailure() {
        val sut = createVenmoClient()
        val paymentAuthResult = simulateVenmoReturn("x-callback-url/vzero/auth/venmo/success")
        assertTrue(paymentAuthResult is VenmoPaymentAuthResult.Success)

        sut.tokenize(paymentAuthResult as VenmoPaymentAuthResult.Success) { result ->
            assertTrue(result is VenmoResult.Failure)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun handleReturnToApp_whenUrlSchemeDoesNotMatch_returnsNoResult() {
        val pendingRequestString = buildPendingRequestString()
        val intent = Intent().apply {
            data = Uri.parse("different-scheme://x-callback-url/venmo/return")
        }

        val result = VenmoLauncher().handleReturnToApp(
            VenmoPendingRequest.Started(pendingRequestString),
            intent
        )

        assertTrue(result is VenmoPaymentAuthResult.NoResult)
    }

    private fun createVenmoClient(): VenmoClient {
        return VenmoClient(
            context = context,
            authorization = Fixtures.TOKENIZATION_KEY,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = returnUrlScheme
        )
    }

    private fun simulateVenmoReturn(returnPath: String): VenmoPaymentAuthResult {
        val pendingRequestString = buildPendingRequestString()
        val intent = Intent().apply {
            data = Uri.parse("$returnUrlScheme://$returnPath")
        }
        return VenmoLauncher().handleReturnToApp(
            VenmoPendingRequest.Started(pendingRequestString),
            intent
        )
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
