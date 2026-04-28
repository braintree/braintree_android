package com.braintreepayments.api.sepadirectdebit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class SEPADirectDebitClientTest {

    private lateinit var context: Context

    companion object {
        private const val SANDBOX_TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn"
        private const val SANDBOX_IBAN = "FR7630006000019876543210173" //generated using the same algorithm as the demo app
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withValidRequest_returnsReadyToLaunchWithBrowserSwitchParams() {
        val returnUrlScheme = "com.braintreepayments.test.sepa"
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, returnUrlScheme)
        val request = buildValidSandboxRequest()

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        val authResult = requireNotNull(result)
        assertTrue(
            "Expected ReadyToLaunch but got ${authResult::class.simpleName}",
            authResult is SEPADirectDebitPaymentAuthRequest.ReadyToLaunch
        )

        val readyToLaunch = authResult as SEPADirectDebitPaymentAuthRequest.ReadyToLaunch
        assertNotNull(readyToLaunch.requestParams)
        assertNotNull(readyToLaunch.requestParams.browserSwitchOptions)

        val options = readyToLaunch.requestParams.browserSwitchOptions
        assertNotNull(options.url)
        assertEquals(BraintreeRequestCodes.SEPA_DEBIT.code, options.requestCode)
        assertEquals(returnUrlScheme, options.returnUrlScheme)

        val metadata = requireNotNull(options.metadata)
        assertTrue(metadata.optString("ibanLastFour").isNotEmpty())
        assertTrue(metadata.optString("customerId").isNotEmpty())
        assertTrue(metadata.optString("bankReferenceToken").isNotEmpty())
        assertTrue(metadata.optString("mandateType").isNotEmpty())
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun endToEnd_successBrowserSwitch_returnsNonceWithDetails() {
        val returnUrlScheme = "com.braintreepayments.test.sepa"
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, returnUrlScheme)
        val request = buildValidSandboxRequest(mandateType = SEPADirectDebitMandateType.RECURRENT)

        val createLatch = CountDownLatch(1)
        var authRequest: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { result ->
            authRequest = result
            createLatch.countDown()
        }
        createLatch.await()

        val createResult = requireNotNull(authRequest)
        assertTrue(
            "Expected ReadyToLaunch but got ${createResult::class.simpleName}",
            createResult is SEPADirectDebitPaymentAuthRequest.ReadyToLaunch
        )
        val readyToLaunch = createResult as SEPADirectDebitPaymentAuthRequest.ReadyToLaunch
        val options = readyToLaunch.requestParams.browserSwitchOptions
        val metadata = requireNotNull(options.metadata)

        val paymentAuthResult = simulateBrowserSwitchReturn(
            returnUrlScheme = returnUrlScheme,
            approvalUrl = options.url.toString(),
            metadata = metadata,
            returnPath = "sepa/success?success=true"
        )

        assertTrue(
            "Expected Success but got ${paymentAuthResult::class.simpleName}",
            paymentAuthResult is SEPADirectDebitPaymentAuthResult.Success
        )

        val tokenizeLatch = CountDownLatch(1)
        var tokenizeResult: SEPADirectDebitResult? = null
        sut.tokenize(paymentAuthResult as SEPADirectDebitPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            tokenizeLatch.countDown()
        }
        tokenizeLatch.await()

        val tokenResult = requireNotNull(tokenizeResult)
        assertTrue(
            "Expected Success but got ${tokenResult::class.simpleName}",
            tokenResult is SEPADirectDebitResult.Success
        )
        val success = tokenResult as SEPADirectDebitResult.Success
        assertNotNull(success.nonce)
        assertTrue(success.nonce.string.isNotEmpty())
        assertNotNull(success.nonce.ibanLastFour)
        assertNotNull(success.nonce.customerId)
        assertNotNull(success.nonce.mandateType)
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun endToEnd_cancelBrowserSwitch_returnsCancelResult() {
        val returnUrlScheme = "com.braintreepayments.test.sepa"
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, returnUrlScheme)
        val request = buildValidSandboxRequest()

        val createLatch = CountDownLatch(1)
        var authRequest: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { result ->
            authRequest = result
            createLatch.countDown()
        }
        createLatch.await()

        val createResult = requireNotNull(authRequest)
        assertTrue(
            "Expected ReadyToLaunch but got ${createResult::class.simpleName}",
            createResult is SEPADirectDebitPaymentAuthRequest.ReadyToLaunch
        )
        val readyToLaunch = createResult as SEPADirectDebitPaymentAuthRequest.ReadyToLaunch
        val options = readyToLaunch.requestParams.browserSwitchOptions
        val metadata = requireNotNull(options.metadata)

        val paymentAuthResult = simulateBrowserSwitchReturn(
            returnUrlScheme = returnUrlScheme,
            approvalUrl = options.url.toString(),
            metadata = metadata,
            returnPath = "sepa/cancel"
        )

        assertTrue(paymentAuthResult is SEPADirectDebitPaymentAuthResult.Success)

        val tokenizeLatch = CountDownLatch(1)
        var tokenizeResult: SEPADirectDebitResult? = null
        sut.tokenize(paymentAuthResult as SEPADirectDebitPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            tokenizeLatch.countDown()
        }
        tokenizeLatch.await()

        val cancelResult = requireNotNull(tokenizeResult)
        assertTrue(
            "Expected Cancel but got ${cancelResult::class.simpleName}",
            cancelResult is SEPADirectDebitResult.Cancel
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withMissingIban_returnsFailure() {
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, null)

        val request = SEPADirectDebitRequest(
            accountHolderName = "John Doe",
            iban = null,
            customerId = "a-customer-id",
            mandateType = SEPADirectDebitMandateType.ONE_OFF,
            merchantAccountId = "EUR-sepa-direct-debit"
        )

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        val failureResult = requireNotNull(result)
        assertTrue(
            "Expected Failure but got ${failureResult::class.simpleName}",
            failureResult is SEPADirectDebitPaymentAuthRequest.Failure
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withMissingAccountHolderName_returnsFailure() {
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, null)

        val request = SEPADirectDebitRequest(
            accountHolderName = null,
            iban = SANDBOX_IBAN,
            customerId = "a-customer-id",
            mandateType = SEPADirectDebitMandateType.ONE_OFF,
            merchantAccountId = "EUR-sepa-direct-debit"
        )

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        val failureResult = requireNotNull(result)
        assertTrue(
            "Expected Failure but got ${failureResult::class.simpleName}",
            failureResult is SEPADirectDebitPaymentAuthRequest.Failure
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withMissingCustomerId_returnsFailure() {
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, null)

        val request = SEPADirectDebitRequest(
            accountHolderName = "John Doe",
            iban = SANDBOX_IBAN,
            customerId = null,
            mandateType = SEPADirectDebitMandateType.ONE_OFF,
            merchantAccountId = "EUR-sepa-direct-debit"
        )

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        val failureResult = requireNotNull(result)
        assertTrue(
            "Expected Failure but got ${failureResult::class.simpleName}",
            failureResult is SEPADirectDebitPaymentAuthRequest.Failure
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withMissingBillingAddress_returnsFailure() {
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, null)

        val request = SEPADirectDebitRequest(
            accountHolderName = "John Doe",
            iban = SANDBOX_IBAN,
            customerId = UUID.randomUUID().toString().substring(0, 20),
            mandateType = SEPADirectDebitMandateType.ONE_OFF,
            billingAddress = null,
            merchantAccountId = "EUR-sepa-direct-debit"
        )

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        val failureResult = requireNotNull(result)
        assertTrue(
            "Expected Failure but got ${failureResult::class.simpleName}",
            failureResult is SEPADirectDebitPaymentAuthRequest.Failure
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withEmptyRequest_returnsFailure() {
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, null)
        val request = SEPADirectDebitRequest()

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        val failureResult = requireNotNull(result)
        assertTrue(
            "Expected Failure but got ${failureResult::class.simpleName}",
            failureResult is SEPADirectDebitPaymentAuthRequest.Failure
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withInvalidAuthorization_returnsFailure() {
        val sut = SEPADirectDebitClient(context, "invalid_authorization_key", null)
        val request = buildValidSandboxRequest()

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        val failureResult = requireNotNull(result)
        assertTrue(
            "Expected Failure but got ${failureResult::class.simpleName}",
            failureResult is SEPADirectDebitPaymentAuthRequest.Failure
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withDisabledPayPal_returnsFailure() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)

        val sut = SEPADirectDebitClient(context, Fixtures.TOKENIZATION_KEY, null)
        val request = buildValidSandboxRequest()

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        val failureResult = requireNotNull(result)
        assertTrue(
            "Expected Failure but got ${failureResult::class.simpleName}",
            failureResult is SEPADirectDebitPaymentAuthRequest.Failure
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_failureResult_containsNonNullError() {
        val sut = SEPADirectDebitClient(context, SANDBOX_TOKENIZATION_KEY, null)
        val request = SEPADirectDebitRequest()

        val latch = CountDownLatch(1)
        var result: SEPADirectDebitPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { paymentAuthRequest ->
            result = paymentAuthRequest
            latch.countDown()
        }

        latch.await()

        assertTrue(result is SEPADirectDebitPaymentAuthRequest.Failure)
        val failure = result as SEPADirectDebitPaymentAuthRequest.Failure
        assertNotNull(failure.error)
        assertNotNull(failure.error.message)
    }

    private fun simulateBrowserSwitchReturn(
        returnUrlScheme: String,
        approvalUrl: String,
        metadata: JSONObject,
        returnPath: String
    ): SEPADirectDebitPaymentAuthResult {
        val pendingRequestJson = JSONObject()
            .put("requestCode", BraintreeRequestCodes.SEPA_DEBIT.code)
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

        val launcher = SEPADirectDebitLauncher()
        val pendingRequest = SEPADirectDebitPendingRequest.Started(pendingRequestString)
        return launcher.handleReturnToApp(pendingRequest, intent)
    }

    private fun buildValidSandboxRequest(
        mandateType: SEPADirectDebitMandateType = SEPADirectDebitMandateType.ONE_OFF
    ): SEPADirectDebitRequest {
        val billingAddress = PostalAddress()
        billingAddress.streetAddress = "Kantstraße 70"
        billingAddress.extendedAddress = "#170"
        billingAddress.locality = "Freistaat Sachsen"
        billingAddress.region = "Annaberg-buchholz"
        billingAddress.postalCode = "09456"
        billingAddress.countryCodeAlpha2 = "FR"

        return SEPADirectDebitRequest(
            accountHolderName = "John Doe",
            iban = SANDBOX_IBAN,
            customerId = UUID.randomUUID().toString().substring(0, 20),
            mandateType = mandateType,
            billingAddress = billingAddress,
            merchantAccountId = "EUR-sepa-direct-debit",
            locale = "en-US"
        )
    }

}
