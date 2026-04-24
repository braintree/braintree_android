package com.braintreepayments.api.localpayment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.PostalAddress
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
class LocalPaymentClientTest {
    private lateinit var context: Context
    private lateinit var countDownLatch: CountDownLatch
    private lateinit var braintreeClient: BraintreeClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        countDownLatch = CountDownLatch(1)
        braintreeClient = BraintreeClient(
            context,
            "sandbox_f252zhq7_hh4cpc39zq4rgjcg"
        )
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_callsBack_withApprovalUrl_andPaymentId() {
        val address = PostalAddress()
        address.streetAddress = "836486 of 22321 Park Lake"
        address.countryCodeAlpha2 = "NL"
        address.locality = "Den Haag"
        address.postalCode = "2585 GJ"

        val request = LocalPaymentRequest(true,
            address = address,
            amount = "1.10",
            currencyCode = "EUR",
            email = "jon@getbraintree.com",
            givenName = "Jon",
            merchantAccountId = "altpay_eur",
            phone = "639847934",
            paymentType = "ideal", isShippingAddressRequired = true, paymentTypeCountryCode = "NL")

        val sut = LocalPaymentClient(braintreeClient)
        sut.createPaymentAuthRequest(request) { localPaymentAuthRequest: LocalPaymentAuthRequest ->
            assertTrue(localPaymentAuthRequest is LocalPaymentAuthRequest.ReadyToLaunch)
            assertNotNull((localPaymentAuthRequest as LocalPaymentAuthRequest.ReadyToLaunch).requestParams.approvalUrl)
            assertNotNull(localPaymentAuthRequest.requestParams.paymentId)
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

        val sut = LocalPaymentClient(context, Fixtures.TOKENIZATION_KEY, null)

        val request = LocalPaymentRequest(
            hasUserLocationConsent = true,
            amount = "1.10",
            currencyCode = "EUR",
            paymentType = "ideal"
        )

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is LocalPaymentAuthRequest.Failure)
            val failure = requireNotNull(result as LocalPaymentAuthRequest.Failure)
            val errorMessage = requireNotNull(failure.error.message)
            assertTrue(errorMessage.contains("Local payments are not enabled for this merchant."))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_whenAmountIsNull_returnsFailure() {
        val sut = LocalPaymentClient(braintreeClient)

        val request = LocalPaymentRequest(
            hasUserLocationConsent = true,
            amount = null,
            paymentType = "ideal"
        )

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is LocalPaymentAuthRequest.Failure)
            val failure = requireNotNull(result as LocalPaymentAuthRequest.Failure)
            val errorMessage = requireNotNull(failure.error.message)
            assertTrue(errorMessage.contains("paymentType and amount are required"))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_whenPaymentTypeIsNull_returnsFailure() {
        val sut = LocalPaymentClient(braintreeClient)

        val request = LocalPaymentRequest(
            hasUserLocationConsent = true,
            amount = "1.10",
            paymentType = null
        )

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is LocalPaymentAuthRequest.Failure)
            val failure = requireNotNull(result as LocalPaymentAuthRequest.Failure)
            val errorMessage = requireNotNull(failure.error.message)
            assertTrue(errorMessage.contains("paymentType and amount are required"))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun tokenize_afterSuccessfulBrowserSwitch_returnsNonce() {
        val returnUrlScheme = "sample-scheme"
        val sut = LocalPaymentClient(context, "sandbox_f252zhq7_hh4cpc39zq4rgjcg", returnUrlScheme)

        val address = PostalAddress()
        address.streetAddress = "836486 of 22321 Park Lake"
        address.countryCodeAlpha2 = "NL"
        address.locality = "Den Haag"
        address.postalCode = "2585 GJ"

        val request = LocalPaymentRequest(
            hasUserLocationConsent = true,
            address = address,
            amount = "1.10",
            currencyCode = "EUR",
            email = "jon@getbraintree.com",
            givenName = "Jon",
            merchantAccountId = "altpay_eur",
            phone = "639847934",
            paymentType = "ideal",
            isShippingAddressRequired = true,
            paymentTypeCountryCode = "NL"
        )

        val createLatch = CountDownLatch(1)
        var authRequest: LocalPaymentAuthRequest? = null
        sut.createPaymentAuthRequest(request) { result ->
            authRequest = result
            createLatch.countDown()
        }
        createLatch.await()

        assertTrue(authRequest is LocalPaymentAuthRequest.ReadyToLaunch)
        val readyToLaunch = authRequest as LocalPaymentAuthRequest.ReadyToLaunch
        val paymentId = readyToLaunch.requestParams.paymentId

        val authResult = simulateBrowserSwitchReturn(
            returnUrlScheme = returnUrlScheme,
            approvalUrl = readyToLaunch.requestParams.approvalUrl,
            returnPath = "local-payment-success?paymentToken=$paymentId"
        )
        assertTrue(authResult is LocalPaymentAuthResult.Success)

        val tokenizeLatch = CountDownLatch(1)
        var tokenizeResult: LocalPaymentResult? = null
        sut.tokenize(context, authResult as LocalPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            tokenizeLatch.countDown()
        }
        tokenizeLatch.await()

        assertTrue(tokenizeResult is LocalPaymentResult.Success)
        val success = tokenizeResult as LocalPaymentResult.Success
        assertNotNull(success.nonce)
        assertNotNull(success.nonce.string)
        assertTrue(success.nonce.string.isNotEmpty())
    }

    @Test(timeout = 10000)
    @Throws(InterruptedException::class)
    fun tokenize_whenBrowserSwitchCanceled_returnsCancelResult() {
        val returnUrlScheme = "sample-scheme"
        val sut = LocalPaymentClient(context, "sandbox_f252zhq7_hh4cpc39zq4rgjcg", returnUrlScheme)

        val authResult = simulateBrowserSwitchReturn(
            returnUrlScheme = returnUrlScheme,
            approvalUrl = "https://example.com/approval",
            returnPath = "local-payment-cancel"
        )
        assertTrue(authResult is LocalPaymentAuthResult.Success)

        val tokenizeLatch = CountDownLatch(1)
        var tokenizeResult: LocalPaymentResult? = null
        sut.tokenize(context, authResult as LocalPaymentAuthResult.Success) { result ->
            tokenizeResult = result
            tokenizeLatch.countDown()
        }
        tokenizeLatch.await()

        assertTrue(tokenizeResult is LocalPaymentResult.Cancel)
    }

    private fun simulateBrowserSwitchReturn(
        returnUrlScheme: String,
        approvalUrl: String,
        returnPath: String
    ): LocalPaymentAuthResult {
        val metadata = JSONObject()
            .put("merchant-account-id", "altpay_eur")
            .put("payment-type", "ideal")
            .put("has-user-location-consent", true)

        val pendingRequestJson = JSONObject()
            .put("requestCode", BraintreeRequestCodes.LOCAL_PAYMENT.code)
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

        val launcher = LocalPaymentLauncher()
        val pendingRequest = LocalPaymentPendingRequest.Started(pendingRequestString)
        return launcher.handleReturnToApp(pendingRequest, intent)
    }
}
