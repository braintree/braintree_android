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
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayActivityResultContractUnitTest {

    @Test
    fun `createIntent returns Intent with extras`() {
        val googlePayRequest = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        val paymentDataRequest = PaymentDataRequest.fromJson(googlePayRequest.toJson())

        val intentData = GooglePayPaymentAuthRequestParams(1, paymentDataRequest)

        val context = ApplicationProvider.getApplicationContext<Context>()

        val sut = GooglePayActivityResultContract()
        val intent = sut.createIntent(context, intentData)

        assertEquals(1, intent.getIntExtra(GooglePayClient.EXTRA_ENVIRONMENT, 0))
        assertSame(
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
        assertTrue(error is UserCanceledException)
        assertEquals("User canceled Google Pay.", error!!.message)
        assertNull(result.paymentData)
    }

    @Test
    fun `parseResult when RESULT_ERROR returns Google Pay result with error`() {
        val data = Intent()

        val sut = GooglePayActivityResultContract()

        val result = sut.parseResult(AutoResolveHelper.RESULT_ERROR, data)

        val error = result.error
        assertTrue(error is GooglePayException)
        assertEquals(
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
        assertTrue(error is BraintreeException)
        assertEquals("An unexpected error occurred.", error!!.message)
        assertNull(result.paymentData)
    }
}
