package com.braintreepayments.api.paymentactions

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.ExpirationDateHelper
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.TestClientTokenBuilder
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4ClassRunner::class)
class PaymentActionsClientTest {

    @Ignore(
        "Pending sandbox/merchant-sample-server support for driving the " +
            "confirmationMethod/captureMethod combinations. See DTMOBILES-2289.",
    )
    @Test(timeout = 10000)
    fun submitForPaymentAction_autoConfirmAutoCapture_returnsCompleted() {
        val authorization = TestClientTokenBuilder().build()
        val sut = PaymentActionsClient(ApplicationProvider.getApplicationContext(), authorization)

        val card = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = ExpirationDateHelper.validExpirationYear(),
            cvv = "123",
            cardholderName = "Cookie Monster",
        )

        val countDownLatch = CountDownLatch(1)
        sut.submitForPaymentAction(card) { result ->
            assertTrue(result is PaymentActionResult.Completed)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Ignore(
        "Pending sandbox/merchant-sample-server support for driving the " +
            "confirmationMethod/captureMethod combinations. See DTMOBILES-2289.",
    )
    @Test(timeout = 10000)
    fun submitForPaymentAction_manualConfirmAutoCapture_returnsServerActionRequiredConfirm() {
        val authorization = TestClientTokenBuilder().build()
        val sut = PaymentActionsClient(ApplicationProvider.getApplicationContext(), authorization)

        val card = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = ExpirationDateHelper.validExpirationYear(),
            cvv = "123",
            cardholderName = "Cookie Monster",
        )

        val countDownLatch = CountDownLatch(1)
        sut.submitForPaymentAction(card) { result ->
            assertTrue(result is PaymentActionResult.ServerActionRequired)
            assertTrue(
                (result as PaymentActionResult.ServerActionRequired).serverAction == ServerAction.CONFIRM,
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Ignore(
        "Pending sandbox/merchant-sample-server support for driving the " +
            "confirmationMethod/captureMethod combinations. See DTMOBILES-2289.",
    )
    @Test(timeout = 10000)
    fun submitForPaymentAction_autoConfirmManualCapture_returnsServerActionRequiredCapture() {
        val authorization = TestClientTokenBuilder().build()
        val sut = PaymentActionsClient(ApplicationProvider.getApplicationContext(), authorization)

        val card = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = ExpirationDateHelper.validExpirationYear(),
            cvv = "123",
            cardholderName = "Cookie Monster",
        )

        val countDownLatch = CountDownLatch(1)
        sut.submitForPaymentAction(card) { result ->
            assertTrue(result is PaymentActionResult.ServerActionRequired)
            assertTrue(
                (result as PaymentActionResult.ServerActionRequired).serverAction == ServerAction.CAPTURE,
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Ignore(
        "Pending sandbox/merchant-sample-server support for driving the " +
            "confirmationMethod/captureMethod combinations. See DTMOBILES-2289.",
    )
    @Test(timeout = 10000)
    fun submitForPaymentAction_manualConfirmManualCapture_returnsServerActionRequiredConfirm() {
        val authorization = TestClientTokenBuilder().build()
        val sut = PaymentActionsClient(ApplicationProvider.getApplicationContext(), authorization)

        val card = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = ExpirationDateHelper.validExpirationYear(),
            cvv = "123",
            cardholderName = "Cookie Monster",
        )

        val countDownLatch = CountDownLatch(1)
        sut.submitForPaymentAction(card) { result ->
            assertTrue(result is PaymentActionResult.ServerActionRequired)
            assertTrue(
                (result as PaymentActionResult.ServerActionRequired).serverAction == ServerAction.CONFIRM,
            )
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }

    @Test(timeout = 10000)
    fun submitForPaymentAction_usingTokenizationKey_failsWithAuthorizationError() {
        val sut = PaymentActionsClient(
            ApplicationProvider.getApplicationContext(),
            Fixtures.TOKENIZATION_KEY,
        )

        val card = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = ExpirationDateHelper.validExpirationYear(),
            cvv = "123",
        )

        val countDownLatch = CountDownLatch(1)
        sut.submitForPaymentAction(card) { result ->
            assertTrue(result is PaymentActionResult.Failure)
            val failure = result as PaymentActionResult.Failure
            assertTrue(failure.error.message?.contains("403") == true)
            countDownLatch.countDown()
        }

        countDownLatch.await()
    }
}
