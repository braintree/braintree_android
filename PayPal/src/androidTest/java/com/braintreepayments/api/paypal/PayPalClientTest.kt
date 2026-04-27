package com.braintreepayments.api.paypal

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
class PayPalClientTest {

    private lateinit var context: Context
    private lateinit var countDownLatch: CountDownLatch

    private val returnUrlScheme = "com.braintreepayments.api.paypal.test"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        countDownLatch = CountDownLatch(1)
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withCheckoutRequest_returnsReadyToLaunch() {
        val sut = createPayPalClient()

        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true
        )

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is PayPalPaymentAuthRequest.ReadyToLaunch)
            val readyToLaunch = result as PayPalPaymentAuthRequest.ReadyToLaunch
            assertNotNull(readyToLaunch.requestParams.approvalUrl)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withVaultRequest_returnsReadyToLaunch() {
        val sut = createPayPalClient()

        val request = PayPalVaultRequest(hasUserLocationConsent = true)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is PayPalPaymentAuthRequest.ReadyToLaunch)
            val readyToLaunch = result as PayPalPaymentAuthRequest.ReadyToLaunch
            assertNotNull(readyToLaunch.requestParams.approvalUrl)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_whenPayPalDisabled_returnsFailure() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)

        val sut = PayPalClient(
            context,
            Fixtures.TOKENIZATION_KEY,
            Uri.parse("https://example.com/braintree"),
            returnUrlScheme
        )

        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true
        )

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is PayPalPaymentAuthRequest.Failure)
            val failure = result as PayPalPaymentAuthRequest.Failure
            val errorMessage = requireNotNull(failure.error.message)
            assertTrue(errorMessage.contains("PayPal is not enabled"))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withInvalidAuthorization_returnsFailure() {
        val sut = PayPalClient(
            context,
            "sandbox_invalid_key_xxxxxxxx",
            Uri.parse("https://example.com/braintree"),
            returnUrlScheme
        )

        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true
        )

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is PayPalPaymentAuthRequest.Failure)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun tokenize_afterSuccessfulBrowserSwitch_withInvalidPayerID_returnsFailure() {
        val sut = createPayPalClient()

        val request = PayPalCheckoutRequest(
            amount = "1.00",
            hasUserLocationConsent = true
        )

        val createLatch = CountDownLatch(1)
        var authRequest: PayPalPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(context, request) { result ->
            authRequest = result
            createLatch.countDown()
        }
        createLatch.await()

        assertTrue(authRequest is PayPalPaymentAuthRequest.ReadyToLaunch)
        val readyToLaunch = authRequest as PayPalPaymentAuthRequest.ReadyToLaunch
        val approvalUrl = requireNotNull(readyToLaunch.requestParams.approvalUrl)
        val successUrl = requireNotNull(readyToLaunch.requestParams.successUrl)
        val token = requireNotNull(Uri.parse(approvalUrl).getQueryParameter("token"))

        val paymentAuthResult = simulateBrowserSwitchReturn(
            approvalUrl = approvalUrl,
            successUrl = successUrl,
            returnPath = "onetouch/v1/success?token=$token&PayerID=FakePayerID"
        )
        assertTrue(paymentAuthResult is PayPalPaymentAuthResult.Success)

        val tokenizeLatch = CountDownLatch(1)
        var tokenizeResult: PayPalResult? = null
        sut.tokenize(paymentAuthResult as PayPalPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            tokenizeLatch.countDown()
        }
        tokenizeLatch.await()

        assertTrue(tokenizeResult is PayPalResult.Failure)
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun tokenize_whenBrowserSwitchCanceled_returnsCancelResult() {
        val sut = createPayPalClient()

        val approvalUrl = "https://www.sandbox.paypal.com/checkout?token=EC-FAKETOKEN123"
        val successUrl = "$returnUrlScheme://onetouch/v1/success"

        val paymentAuthResult = simulateBrowserSwitchReturn(
            approvalUrl = approvalUrl,
            successUrl = successUrl,
            returnPath = "onetouch/v1/cancel?token=EC-FAKETOKEN123"
        )
        assertTrue(paymentAuthResult is PayPalPaymentAuthResult.Success)

        val tokenizeLatch = CountDownLatch(1)
        var tokenizeResult: PayPalResult? = null
        sut.tokenize(paymentAuthResult as PayPalPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            tokenizeLatch.countDown()
        }
        tokenizeLatch.await()

        assertTrue(tokenizeResult is PayPalResult.Cancel)
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun tokenize_whenTokenMismatch_returnsFailure() {
        val sut = createPayPalClient()

        val approvalUrl = "https://www.sandbox.paypal.com/checkout?token=EC-ORIGINALTOKEN"
        val successUrl = "$returnUrlScheme://onetouch/v1/success"

        val paymentAuthResult = simulateBrowserSwitchReturn(
            approvalUrl = approvalUrl,
            successUrl = successUrl,
            returnPath = "onetouch/v1/success?token=EC-DIFFERENTTOKEN"
        )
        assertTrue(paymentAuthResult is PayPalPaymentAuthResult.Success)

        val tokenizeLatch = CountDownLatch(1)
        var tokenizeResult: PayPalResult? = null
        sut.tokenize(paymentAuthResult as PayPalPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            tokenizeLatch.countDown()
        }
        tokenizeLatch.await()

        assertTrue(tokenizeResult is PayPalResult.Failure)
        val failure = tokenizeResult as PayPalResult.Failure
        val errorMessage = requireNotNull(failure.error.message)
        assertTrue(errorMessage.contains("inconsistent data"))
    }

    private fun createPayPalClient(): PayPalClient {
        return PayPalClient(
            context,
            "sandbox_f252zhq7_hh4cpc39zq4rgjcg",
            Uri.parse("https://example.com/braintree"),
            returnUrlScheme
        )
    }

    private fun simulateBrowserSwitchReturn(
        approvalUrl: String,
        successUrl: String,
        returnPath: String
    ): PayPalPaymentAuthResult {
        val metadata = JSONObject()
            .put("approval-url", approvalUrl)
            .put("success-url", successUrl)
            .put("payment-type", "single-payment")
            .put("client-metadata-id", "fake-client-metadata-id")
            .put("merchant-account-id", null)
            .put("source", "paypal-browser")
            .put("intent", "authorize")

        val pendingRequestJson = JSONObject()
            .put("requestCode", BraintreeRequestCodes.PAYPAL.code)
            .put("url", approvalUrl)
            .put("returnUrlScheme", returnUrlScheme)
            .put("metadata", metadata)

        val pendingRequestString = Base64.encodeToString(
            pendingRequestJson.toString().toByteArray(Charsets.UTF_8),
            Base64.DEFAULT
        )

        val intent = Intent().apply {
            data = Uri.parse("$returnUrlScheme://$returnPath")
        }

        val launcher = PayPalLauncher()
        val pendingRequest = PayPalPendingRequest.Started(pendingRequestString)
        return launcher.handleReturnToApp(pendingRequest, intent)
    }
}