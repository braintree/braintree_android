package com.braintreepayments.api.googlepay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.WalletConstants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class GooglePayClientTest {

    private lateinit var context: Context
    private lateinit var sut: GooglePayClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)
        sut = GooglePayClient(context, Fixtures.TOKENIZATION_KEY)
    }

    @Test(timeout = 10000)
    fun getTokenizationParameters_withTokenizationKey_returnsCorrectParameters() {
        val countDownLatch = CountDownLatch(1)

        sut.getTokenizationParameters { result ->
            assertTrue(result is GooglePayTokenizationParameters.Success)
            val success = result as GooglePayTokenizationParameters.Success
            val params = success.parameters
            assertEquals("braintree", params.parameters.getString("gateway"))
            assertEquals("integration_merchant_id", params.parameters.getString("braintree:merchantId"))
            assertEquals("v1", params.parameters.getString("braintree:apiVersion"))
            assertEquals(
                "google-auth-fingerprint",
                params.parameters.getString("braintree:authorizationFingerprint")
            )
            assertEquals(
                Fixtures.TOKENIZATION_KEY,
                params.parameters.getString("braintree:clientKey")
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun getTokenizationParameters_returnsAllowedCardNetworks() {
        val countDownLatch = CountDownLatch(1)

        sut.getTokenizationParameters { result ->
            assertTrue(result is GooglePayTokenizationParameters.Success)
            val success = result as GooglePayTokenizationParameters.Success
            val allowedCardNetworks = success.allowedCardNetworks
            assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_VISA))
            assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_MASTERCARD))
            assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_AMEX))
            assertTrue(allowedCardNetworks.contains(WalletConstants.CARD_NETWORK_DISCOVER))
            assertEquals(4, allowedCardNetworks.size)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_whenGooglePayEnabled_returnsReadyToLaunch() {
        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "1.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL
        )

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is GooglePayPaymentAuthRequest.ReadyToLaunch)
            val readyToLaunch = result as GooglePayPaymentAuthRequest.ReadyToLaunch
            val params = readyToLaunch.requestParams
            assertEquals(WalletConstants.ENVIRONMENT_TEST, params.googlePayEnvironment)
            assertNotNull(params.paymentDataRequest)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_whenProduction_setsProductionEnvironment() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY_PRODUCTION)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)

        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "1.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL
        )

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is GooglePayPaymentAuthRequest.ReadyToLaunch)
            val readyToLaunch = result as GooglePayPaymentAuthRequest.ReadyToLaunch
            assertEquals(
                WalletConstants.ENVIRONMENT_PRODUCTION,
                readyToLaunch.requestParams.googlePayEnvironment
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_whenGooglePayNotEnabled_returnsFailure() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_GOOGLE_PAY)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)

        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "1.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL
        )

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is GooglePayPaymentAuthRequest.Failure)
            val failure = result as GooglePayPaymentAuthRequest.Failure
            val message = requireNotNull(failure.error.message) { "Error message should not be null" }
            assertTrue(message.contains("Google Pay is not enabled"))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_includesAllowedPaymentMethods() {
        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "10.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL,
            isBillingAddressRequired = true,
            billingAddressFormat = GooglePayBillingAddressFormat.FULL,
            isEmailRequired = true
        )

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is GooglePayPaymentAuthRequest.ReadyToLaunch)
            val readyToLaunch = result as GooglePayPaymentAuthRequest.ReadyToLaunch
            val paymentDataRequestJson =
                JSONObject(readyToLaunch.requestParams.paymentDataRequest.toJson())

            assertTrue(paymentDataRequestJson.getBoolean("emailRequired"))

            val allowedPaymentMethods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")
                .toJsonObjectList()
            assertTrue(allowedPaymentMethods.isNotEmpty())

            val card = allowedPaymentMethods.first { it.getString("type") == "CARD" }
            assertTrue(card.getJSONObject("parameters").getBoolean("billingAddressRequired"))

            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_includesPayPalWhenEnabled() {
        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "1.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL,
            isPayPalEnabled = true
        )

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is GooglePayPaymentAuthRequest.ReadyToLaunch)
            val readyToLaunch = result as GooglePayPaymentAuthRequest.ReadyToLaunch
            val paymentDataRequestJson =
                JSONObject(readyToLaunch.requestParams.paymentDataRequest.toJson())
            val methods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")
                .toJsonObjectList()

            assertTrue(methods.any { it.getString("type") == "PAYPAL" })

            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_excludesPayPalWhenDisabled() {
        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "1.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL,
            isPayPalEnabled = false
        )

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is GooglePayPaymentAuthRequest.ReadyToLaunch)
            val readyToLaunch = result as GooglePayPaymentAuthRequest.ReadyToLaunch
            val paymentDataRequestJson =
                JSONObject(readyToLaunch.requestParams.paymentDataRequest.toJson())
            val methods = paymentDataRequestJson.getJSONArray("allowedPaymentMethods")
                .toJsonObjectList()

            assertTrue(methods.none { it.getString("type") == "PAYPAL" })

            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_includesTransactionInfo() {
        val request = GooglePayRequest(
            currencyCode = "EUR",
            totalPrice = "25.50",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_ESTIMATED,
            countryCode = "DE",
            googleMerchantName = "Test Merchant"
        )

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(request) { result ->
            assertTrue(result is GooglePayPaymentAuthRequest.ReadyToLaunch)
            val readyToLaunch = result as GooglePayPaymentAuthRequest.ReadyToLaunch
            val paymentDataRequestJson =
                JSONObject(readyToLaunch.requestParams.paymentDataRequest.toJson())

            val transactionInfo = paymentDataRequestJson.getJSONObject("transactionInfo")
            assertEquals("EUR", transactionInfo.getString("currencyCode"))
            assertEquals("25.50", transactionInfo.getString("totalPrice"))
            assertEquals("ESTIMATED", transactionInfo.getString("totalPriceStatus"))
            assertEquals("DE", transactionInfo.getString("countryCode"))

            val merchantInfo = paymentDataRequestJson.getJSONObject("merchantInfo")
            assertEquals("Test Merchant", merchantInfo.getString("merchantName"))

            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test
    fun tokenize_withCardPaymentData_returnsGooglePayCardNonce() {
        val paymentData = PaymentData.fromJson(Fixtures.RESPONSE_GOOGLE_PAY_CARD)

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentData) { result ->
            assertTrue(result is GooglePayResult.Success)
            val success = result as GooglePayResult.Success
            assertTrue(success.nonce is GooglePayCardNonce)

            val nonce = success.nonce as GooglePayCardNonce
            assertEquals("Visa", nonce.cardType)
            assertEquals("1234", nonce.lastFour)
            assertEquals("34", nonce.lastTwo)
            assertEquals("123456", nonce.bin)
            assertEquals("VISA", nonce.cardNetwork)

            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test
    fun tokenize_withPayPalPaymentData_returnsPayPalAccountNonce() {
        val paymentData = PaymentData.fromJson(Fixtures.REPSONSE_GOOGLE_PAY_PAYPAL_ACCOUNT)

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentData) { result ->
            assertTrue(result is GooglePayResult.Success)
            val success = result as GooglePayResult.Success
            assertTrue(success.nonce is PayPalAccountNonce)

            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test
    fun tokenize_withUserCanceledError_returnsCancel() {
        val paymentAuthResult = GooglePayPaymentAuthResult(
            paymentData = null,
            error = UserCanceledException("User canceled Google Pay.")
        )

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentAuthResult) { result ->
            assertTrue(result is GooglePayResult.Cancel)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test
    fun tokenize_withError_returnsFailure() {
        val error = Exception("Something went wrong")
        val paymentAuthResult = GooglePayPaymentAuthResult(
            paymentData = null,
            error = error
        )

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentAuthResult) { result ->
            assertTrue(result is GooglePayResult.Failure)
            val failure = result as GooglePayResult.Failure
            assertEquals("Something went wrong", failure.error.message)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    private fun JSONArray.toJsonObjectList(): List<JSONObject> =
        (0 until length()).map { getJSONObject(it) }
}
