package com.braintreepayments.api.googlepay

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.UserCanceledException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import junit.framework.TestCase
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayActivityResultContractUnitTest {

    @Test
    fun `createIntent returns Intent with extras`() {
        val googlePayRequest = GooglePayRequest()
        googlePayRequest.transactionInfo = TransactionInfo.newBuilder()
            .setTotalPrice("1.00")
            .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
            .setCurrencyCode("USD")
            .build()

        val paymentDataRequest = PaymentDataRequest.fromJson(googlePayRequest.toJson())

        val intentData = GooglePayPaymentAuthRequestParams(1, paymentDataRequest)

        val context = ApplicationProvider.getApplicationContext<Context>()

        val sut = GooglePayActivityResultContract()
        val intent = sut.createIntent(context, intentData)

        TestCase.assertEquals(1, intent.getIntExtra(GooglePayClient.EXTRA_ENVIRONMENT, 0))
        TestCase.assertSame(
            paymentDataRequest,
            intent.getParcelableExtra(GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST)
        )
    }

    @Test
    fun `parseResult when result is OK and payment data exists returns Google Pay result with PaymentData`() {
        val paymentData = PaymentData.fromJson("{}")
        val data = Intent()
        paymentData.putIntoIntent(data)

        val sut = GooglePayActivityResultContract()

        val result = sut.parseResult(Activity.RESULT_OK, data)
        assertNotNull(result.paymentData)
        assertNull(result.error)
    }

    @Test
    fun `parseResult when result is canceled returns Google Pay result with error`() {
        val data = Intent()

        val sut = GooglePayActivityResultContract()

        val result = sut.parseResult(Activity.RESULT_CANCELED, data)

        val error = result.error
        TestCase.assertTrue(error is UserCanceledException)
        TestCase.assertEquals("User canceled Google Pay.", error!!.message)
        assertNull(result.paymentData)
    }

    @Test
    fun `parseResult when RESULT_ERROR returns Google Pay result with error`() {
        val data = Intent()

        val sut = GooglePayActivityResultContract()

        val result = sut.parseResult(AutoResolveHelper.RESULT_ERROR, data)

        val error = result.error
        TestCase.assertTrue(error is GooglePayException)
        TestCase.assertEquals(
            "An error was encountered during the Google Pay " +
                    "flow. See the status object in this exception for more details.", error!!.message
        )
        assertNull(result.paymentData)
    }

    @Test
    fun `parseResult when result is unexpected returns Google Pay result with error`() {
        val data = Intent()

        val sut = GooglePayActivityResultContract()

        val result = sut.parseResult(2, data)

        val error = result.error
        TestCase.assertTrue(error is BraintreeException)
        TestCase.assertEquals("An unexpected error occurred.", error!!.message)
        assertNull(result.paymentData)
    }
}
