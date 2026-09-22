package com.braintreepayments.api.paymentactions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.ExpirationDateHelper
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.PaymentActionCaptureMethod
import com.braintreepayments.api.testutils.PaymentActionClientTokenBuilder
import com.braintreepayments.api.testutils.PaymentActionConfirmationMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
class PaymentActionsClientTest {

    private lateinit var context: Context
    private lateinit var countDownLatch: CountDownLatch
    private lateinit var card: CreditCard

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        countDownLatch = CountDownLatch(1)
        card = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = ExpirationDateHelper.validExpirationYear(),
            cvv = "123",
            cardholderName = "Cookie Monster",
            streetAddress = "123 Sesame Street",
            extendedAddress = "Apt 5B",
            locality = "New York",
            region = "NY",
            postalCode = "10001",
            countryCodeAlpha2 = "US",
        )
    }

    @Test(timeout = 30000)
    fun submitForPaymentAction_autoConfirmAutoCapture_returnsCompleted() {
        val createPaymentActionResponse = PaymentActionClientTokenBuilder().build()
        val sut = PaymentActionsClient(context, createPaymentActionResponse.clientToken)

        var result: PaymentActionResult? = null
        sut.submitForPaymentAction(card) {
            result = it
            countDownLatch.countDown()
        }

        assertTrue("callback never fired", countDownLatch.await(25, TimeUnit.SECONDS))
        assertEquals(PaymentActionResult.Completed::class.java, result?.javaClass)
        val outcome = result as PaymentActionResult.Completed
        assertEquals(createPaymentActionResponse.paymentAction.id, outcome.id)
    }

    @Test(timeout = 30000)
    fun submitForPaymentAction_manualConfirmAutoCapture_returnsServerActionRequiredConfirm() {
        val createPaymentActionResponse = PaymentActionClientTokenBuilder()
            .confirmationMethod(PaymentActionConfirmationMethod.MANUAL)
            .build()
        val sut = PaymentActionsClient(context, createPaymentActionResponse.clientToken)

        var result: PaymentActionResult? = null
        sut.submitForPaymentAction(card) {
            result = it
            countDownLatch.countDown()
        }

        assertTrue("callback never fired", countDownLatch.await(25, TimeUnit.SECONDS))
        assertEquals(PaymentActionResult.ServerActionRequired::class.java, result?.javaClass)
        val outcome = result as PaymentActionResult.ServerActionRequired
        assertEquals(createPaymentActionResponse.paymentAction.id, outcome.id)
        assertEquals(ServerAction.CONFIRM, outcome.serverAction)
    }

    @Test(timeout = 30000)
    fun submitForPaymentAction_autoConfirmManualCapture_returnsServerActionRequiredCapture() {
        val createPaymentActionResponse = PaymentActionClientTokenBuilder()
            .captureMethod(PaymentActionCaptureMethod.MANUAL)
            .build()
        val sut = PaymentActionsClient(context, createPaymentActionResponse.clientToken)

        var result: PaymentActionResult? = null
        sut.submitForPaymentAction(card) {
            result = it
            countDownLatch.countDown()
        }

        assertTrue("callback never fired", countDownLatch.await(25, TimeUnit.SECONDS))
        assertEquals(PaymentActionResult.ServerActionRequired::class.java, result?.javaClass)
        val outcome = result as PaymentActionResult.ServerActionRequired
        assertEquals(createPaymentActionResponse.paymentAction.id, outcome.id)
        assertEquals(ServerAction.CAPTURE, outcome.serverAction)
    }

    @Test(timeout = 30000)
    fun submitForPaymentAction_manualConfirmManualCapture_returnsServerActionRequiredConfirm() {
        val createPaymentActionResponse = PaymentActionClientTokenBuilder()
            .confirmationMethod(PaymentActionConfirmationMethod.MANUAL)
            .captureMethod(PaymentActionCaptureMethod.MANUAL)
            .build()
        val sut = PaymentActionsClient(context, createPaymentActionResponse.clientToken)

        var result: PaymentActionResult? = null
        sut.submitForPaymentAction(card) {
            result = it
            countDownLatch.countDown()
        }

        assertTrue("callback never fired", countDownLatch.await(25, TimeUnit.SECONDS))
        assertEquals(PaymentActionResult.ServerActionRequired::class.java, result?.javaClass)
        val outcome = result as PaymentActionResult.ServerActionRequired
        assertEquals(createPaymentActionResponse.paymentAction.id, outcome.id)
        assertEquals(ServerAction.CONFIRM, outcome.serverAction)
    }

    @Test(timeout = 30000)
    fun submitForPaymentAction_withInvalidAuthorization_returnsFailure() {
        val sut = PaymentActionsClient(context, "invalid_authorization_key")

        var result: PaymentActionResult? = null
        sut.submitForPaymentAction(card) {
            result = it
            countDownLatch.countDown()
        }

        assertTrue("callback never fired", countDownLatch.await(25, TimeUnit.SECONDS))
        assertEquals(PaymentActionResult.Failure::class.java, result?.javaClass)
    }

    @Test(timeout = 30000)
    fun submitForPaymentAction_withTokenizationKey_returnsFailure() {
        val sut = PaymentActionsClient(context, Fixtures.TOKENIZATION_KEY)

        var result: PaymentActionResult? = null
        sut.submitForPaymentAction(card) {
            result = it
            countDownLatch.countDown()
        }

        assertTrue("callback never fired", countDownLatch.await(25, TimeUnit.SECONDS))
        assertEquals(PaymentActionResult.Failure::class.java, result?.javaClass)
    }

    @Test(timeout = 30000)
    fun submitForPaymentAction_withAlreadyUsedClientToken_returnsFailure() {
        val createPaymentActionResponse = PaymentActionClientTokenBuilder().build()
        val sut = PaymentActionsClient(context, createPaymentActionResponse.clientToken)

        val firstResult = CountDownLatch(1)
        var result: PaymentActionResult? = null
        sut.submitForPaymentAction(card) {
            result = it
            firstResult.countDown()
        }
        assertTrue("callback never fired", firstResult.await(25, TimeUnit.SECONDS))
        assertEquals(PaymentActionResult.Completed::class.java, result?.javaClass)

        val secondResult = CountDownLatch(1)
        var retryResult: PaymentActionResult? = null
        sut.submitForPaymentAction(card) {
            retryResult = it
            secondResult.countDown()
        }
        assertTrue("callback never fired", secondResult.await(25, TimeUnit.SECONDS))
        assertEquals(PaymentActionResult.Failure::class.java, retryResult?.javaClass)
    }
}
