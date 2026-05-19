package com.braintreepayments.api.threedsecure

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardResult
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.testutils.CardNumber
import com.braintreepayments.api.testutils.ExpirationDateHelper
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import com.braintreepayments.api.testutils.TestClientTokenBuilder
import com.cardinalcommerce.cardinalmobilesdk.cm.models.CardinalError
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class ThreeDSecureClientTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_withNullAmount_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val request = ThreeDSecureRequest(nonce = "fake-nonce", amount = null)

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.Failure)
            val failure = result as ThreeDSecurePaymentAuthRequest.Failure
            assertTrue(failure.error.message.orEmpty().contains("nonce and amount cannot be null"))
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_withNullNonce_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val request = ThreeDSecureRequest(nonce = null, amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.Failure)
            assertTrue(
                (result as ThreeDSecurePaymentAuthRequest.Failure)
                    .error.message.orEmpty().contains("nonce and amount cannot be null")
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_whenThreeDSecureDisabled_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITHOUT_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val request = ThreeDSecureRequest(nonce = "fake-nonce", amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.Failure)
            assertTrue(
                (result as ThreeDSecurePaymentAuthRequest.Failure)
                    .error.message.orEmpty().contains("Three D Secure is not enabled")
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun createPaymentAuthRequest_whenMissingCardinalJwt_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val request = ThreeDSecureRequest(nonce = "fake-nonce", amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.Failure)
            assertTrue(
                (result as ThreeDSecurePaymentAuthRequest.Failure)
                    .error.message.orEmpty().contains("Merchant is not configured for 3DS 2.0")
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun initializeChallengeWithLookupResponse_withInvalidJson_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val countDownLatch = CountDownLatch(1)

        sut.initializeChallengeWithLookupResponse("not valid json") { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.Failure)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun initializeChallengeWithLookupResponse_withNoPaymentMethodOrLookup_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val countDownLatch = CountDownLatch(1)

        sut.initializeChallengeWithLookupResponse(
            Fixtures.THREE_D_SECURE_V2_AUTHENTICATION_RESPONSE_WITH_ERROR
        ) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.Failure)
            assertTrue(
                (result as ThreeDSecurePaymentAuthRequest.Failure)
                    .error.message.orEmpty().contains("lookup and threeDSecureNonce are null")
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun initializeChallengeWithLookupResponse_withNonceButNoLookup_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val countDownLatch = CountDownLatch(1)

        sut.initializeChallengeWithLookupResponse(
            Fixtures.THREE_D_SECURE_AUTHENTICATION_RESPONSE
        ) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.Failure)
            assertTrue(
                (result as ThreeDSecurePaymentAuthRequest.Failure)
                    .error.message.orEmpty().contains("lookup and threeDSecureNonce are null")
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun initializeChallengeWithLookupResponse_withV1Response_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val countDownLatch = CountDownLatch(1)

        sut.initializeChallengeWithLookupResponse(
            Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE
        ) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.Failure)
            assertTrue(
                (result as ThreeDSecurePaymentAuthRequest.Failure)
                    .error.message.orEmpty().contains("3D Secure v1 is deprecated")
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun initializeChallengeWithLookupResponse_withV2AcsUrl_returnsReadyToLaunch() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val countDownLatch = CountDownLatch(1)

        sut.initializeChallengeWithLookupResponse(
            Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE
        ) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.ReadyToLaunch)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun initializeChallengeWithLookupResponse_withNoAcsUrl_returnsLaunchNotRequiredWithNonceDetails() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val countDownLatch = CountDownLatch(1)

        sut.initializeChallengeWithLookupResponse(
            Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL
        ) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.LaunchNotRequired)
            val nonce = (result as ThreeDSecurePaymentAuthRequest.LaunchNotRequired).nonce
            assertEquals("Visa", nonce.cardType)
            assertEquals("11", nonce.lastTwo)
            assertEquals("1111", nonce.lastFour)
            assertTrue(nonce.threeDSecureInfo.liabilityShifted)
            assertTrue(nonce.threeDSecureInfo.liabilityShiftPossible)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun tokenize_withError_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val paymentAuthResult = ThreeDSecurePaymentAuthResult(
            error = BraintreeException("3DS challenge failed")
        )

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentAuthResult) { result ->
            assertTrue(result is ThreeDSecureResult.Failure)
            val failure = result as ThreeDSecureResult.Failure
            assertEquals("3DS challenge failed", failure.error.message)
            assertNull(failure.nonce)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun tokenize_whenCanceled_returnsCancel() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val paymentAuthResult = ThreeDSecurePaymentAuthResult(
            validateResponse = ValidateResponse(false, CardinalActionCode.CANCEL, CardinalError(0, "User canceled"))
        )

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentAuthResult) { result ->
            assertTrue(result is ThreeDSecureResult.Cancel)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun tokenize_whenErrorOrTimeout_returnsFailureWithNullNonce() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val paymentAuthResult = ThreeDSecurePaymentAuthResult(
            validateResponse = ValidateResponse(false, CardinalActionCode.TIMEOUT, CardinalError(0, "Timeout occurred"))
        )

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentAuthResult) { result ->
            assertTrue(result is ThreeDSecureResult.Failure)
            assertNull((result as ThreeDSecureResult.Failure).nonce)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun tokenize_whenSuccess_withNullParams_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val paymentAuthResult = ThreeDSecurePaymentAuthResult(
            validateResponse = ValidateResponse(true, CardinalActionCode.SUCCESS, CardinalError(0)),
            threeDSecureParams = null,
            jwt = null
        )

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentAuthResult) { result ->
            assertTrue(result is ThreeDSecureResult.Failure)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun tokenize_withNullValidateResponse_returnsInvalidActionCodeFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val paymentAuthResult = ThreeDSecurePaymentAuthResult(
            validateResponse = null,
            threeDSecureParams = null,
            jwt = null
        )

        val countDownLatch = CountDownLatch(1)

        sut.tokenize(paymentAuthResult) { result ->
            assertTrue(result is ThreeDSecureResult.Failure)
            assertEquals("invalid action code", (result as ThreeDSecureResult.Failure).error.message)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun prepareLookup_whenMissingCardinalJwt_returnsFailure() {
        overrideConfigCache(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)

        val sut = ThreeDSecureClient(context, Fixtures.TOKENIZATION_KEY)
        val request = ThreeDSecureRequest(nonce = "fake-nonce", amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.prepareLookup(context, request) { result ->
            assertTrue(result is ThreeDSecurePrepareLookupResult.Failure)
            assertTrue(
                (result as ThreeDSecurePrepareLookupResult.Failure)
                    .error.message.orEmpty().contains("Merchant is not configured for 3DS 2.0")
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun prepareLookup_withRealSandbox_returnsSuccessWithClientData() {
        assumeTrue("Cardinal SDK not compatible with Android 15+", Build.VERSION.SDK_INT < 35)

        val authorization = TestClientTokenBuilder().build()
        val cardNonce = tokenizeCard(authorization, CardNumber.THREE_D_SECURE_VERIFICATON_NOT_REQUIRED)

        val sut = ThreeDSecureClient(context, authorization)
        val request = ThreeDSecureRequest(nonce = cardNonce, amount = "25.00", email = "test@example.com")

        val countDownLatch = CountDownLatch(1)

        sut.prepareLookup(context, request) { result ->
            assertTrue(
                "Expected Success but got Failure: " +
                    "${(result as? ThreeDSecurePrepareLookupResult.Failure)?.error?.message}",
                result is ThreeDSecurePrepareLookupResult.Success
            )
            val success = result as ThreeDSecurePrepareLookupResult.Success

            val clientDataJson = JSONObject(success.clientData)
            assertTrue(clientDataJson.has("authorizationFingerprint"))
            assertTrue(clientDataJson.has("braintreeLibraryVersion"))
            assertEquals(cardNonce, clientDataJson.getString("nonce"))
            assertTrue(clientDataJson.getString("dfReferenceId").isNotEmpty())
            assertEquals("2", clientDataJson.getJSONObject("clientMetadata").getString("requestedThreeDSecureVersion"))

            assertEquals(cardNonce, success.request.nonce)
            assertEquals("25.00", success.request.amount)
            assertEquals("test@example.com", success.request.email)

            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withVerificationNotRequiredCard_returnsLaunchNotRequired() {
        assumeTrue("Cardinal SDK not compatible with Android 15+", Build.VERSION.SDK_INT < 35)

        val authorization = TestClientTokenBuilder().build()
        val cardNonce = tokenizeCard(authorization, CardNumber.THREE_D_SECURE_VERIFICATON_NOT_REQUIRED)

        val sut = ThreeDSecureClient(context, authorization)
        val request = ThreeDSecureRequest(nonce = cardNonce, amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.LaunchNotRequired)
            assertNotNull((result as ThreeDSecurePaymentAuthRequest.LaunchNotRequired).nonce)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withVerificationCard_completesLookupSuccessfully() {
        assumeTrue("Cardinal SDK not compatible with Android 15+", Build.VERSION.SDK_INT < 35)

        val authorization = TestClientTokenBuilder().build()
        val cardNonce = tokenizeCard(authorization, CardNumber.THREE_D_SECURE_VERIFICATON)

        val sut = ThreeDSecureClient(context, authorization)
        val request = ThreeDSecureRequest(nonce = cardNonce, amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertFalse(
                "Expected non-failure but got: ${(result as? ThreeDSecurePaymentAuthRequest.Failure)?.error?.message}",
                result is ThreeDSecurePaymentAuthRequest.Failure
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withLookupErrorCard_completesWithoutCrash() {
        assumeTrue("Cardinal SDK not compatible with Android 15+", Build.VERSION.SDK_INT < 35)

        val authorization = TestClientTokenBuilder().build()
        val cardNonce = tokenizeCard(authorization, CardNumber.THREE_D_SECURE_LOOKUP_ERROR)

        val sut = ThreeDSecureClient(context, authorization)
        val request = ThreeDSecureRequest(nonce = cardNonce, amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertNotNull(result)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withAuthFailedCard_returnsLaunchNotRequiredWithNoLiabilityShift() {
        assumeTrue("Cardinal SDK not compatible with Android 15+", Build.VERSION.SDK_INT < 35)

        val authorization = TestClientTokenBuilder().build()
        val cardNonce = tokenizeCard(authorization, CardNumber.THREE_D_SECURE_AUTHENTICATION_FAILED)

        val sut = ThreeDSecureClient(context, authorization)
        val request = ThreeDSecureRequest(nonce = cardNonce, amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.LaunchNotRequired)
            assertFalse(
                (result as ThreeDSecurePaymentAuthRequest.LaunchNotRequired)
                    .nonce.threeDSecureInfo.liabilityShifted
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withAuthenticationUnavailableCard_returnsLaunchNotRequired() {
        assumeTrue("Cardinal SDK not compatible with Android 15+", Build.VERSION.SDK_INT < 35)

        val authorization = TestClientTokenBuilder().build()
        val cardNonce = tokenizeCard(authorization, CardNumber.THREE_D_SECURE_AUTHENTICATION_UNAVAILABLE)

        val sut = ThreeDSecureClient(context, authorization)
        val request = ThreeDSecureRequest(nonce = cardNonce, amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.LaunchNotRequired)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createPaymentAuthRequest_withVerificationNotRequired_nonceHasThreeDSecureInfo() {
        assumeTrue("Cardinal SDK not compatible with Android 15+", Build.VERSION.SDK_INT < 35)

        val authorization = TestClientTokenBuilder().build()
        val cardNonce = tokenizeCard(authorization, CardNumber.THREE_D_SECURE_VERIFICATON_NOT_REQUIRED)

        val sut = ThreeDSecureClient(context, authorization)
        val request = ThreeDSecureRequest(nonce = cardNonce, amount = "10.00")

        val countDownLatch = CountDownLatch(1)

        sut.createPaymentAuthRequest(context, request) { result ->
            assertTrue(result is ThreeDSecurePaymentAuthRequest.LaunchNotRequired)
            val nonce = (result as ThreeDSecurePaymentAuthRequest.LaunchNotRequired).nonce
            assertNotNull(nonce.threeDSecureInfo)
            assertTrue(nonce.threeDSecureInfo.wasVerified)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    private fun overrideConfigCache(configJson: String) {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(configJson)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)
    }

    private fun tokenizeCard(authorization: String, cardNumber: String): String {
        val cardClient = CardClient(context, authorization)

        val card = Card()
        card.number = cardNumber
        card.expirationMonth = "01"
        card.expirationYear = ExpirationDateHelper.validExpirationYear()
        card.cvv = "123"

        val countDownLatch = CountDownLatch(1)
        var nonce = ""

        cardClient.tokenize(card) { result ->
            assertTrue(
                "Card tokenization failed: ${(result as? CardResult.Failure)?.error?.message}",
                result is CardResult.Success
            )
            nonce = (result as CardResult.Success).nonce.string
            countDownLatch.countDown()
        }

        countDownLatch.await()
        return nonce
    }
}
