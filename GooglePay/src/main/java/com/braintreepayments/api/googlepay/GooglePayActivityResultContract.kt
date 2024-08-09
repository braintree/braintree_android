package com.braintreepayments.api.googlepay

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.UserCanceledException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData

internal class GooglePayActivityResultContract :
    ActivityResultContract<GooglePayPaymentAuthRequestParams, GooglePayPaymentAuthResult>() {
    override fun createIntent(context: Context, input: GooglePayPaymentAuthRequestParams): Intent {
        return Intent(context, GooglePayActivity::class.java)
            .putExtra(GooglePayClient.EXTRA_ENVIRONMENT, input.googlePayEnvironment)
            .putExtra(GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST, input.paymentDataRequest)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): GooglePayPaymentAuthResult {
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                return GooglePayPaymentAuthResult(PaymentData.getFromIntent(intent), null)
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            return GooglePayPaymentAuthResult(
                null,
                UserCanceledException("User canceled Google Pay.")
            )
        } else if (resultCode == AutoResolveHelper.RESULT_ERROR) {
            if (intent != null) {
                return GooglePayPaymentAuthResult(
                    null, GooglePayException(
                        "An error was encountered during the Google Pay " +
                                "flow. See the status object in this exception for more details.",
                        AutoResolveHelper.getStatusFromIntent(intent)
                    )
                )
            }
        }
        return GooglePayPaymentAuthResult(null, BraintreeException("An unexpected error occurred."))
    }
}
