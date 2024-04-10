package com.braintreepayments.api.googlepay;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.googlepay.GooglePayClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.googlepay.GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST;
import static com.google.android.gms.wallet.AutoResolveHelper.RESULT_ERROR;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braintreepayments.api.core.BraintreeException;
import com.braintreepayments.api.core.UserCanceledException;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.PaymentData;

class GooglePayActivityResultContract extends ActivityResultContract<GooglePayPaymentAuthRequestParams, GooglePayPaymentAuthResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, GooglePayPaymentAuthRequestParams input) {
        return new Intent(context, GooglePayActivity.class)
                .putExtra(EXTRA_ENVIRONMENT, input.getGooglePayEnvironment())
                .putExtra(EXTRA_PAYMENT_DATA_REQUEST, input.getPaymentDataRequest());
    }

    @Override
    public GooglePayPaymentAuthResult parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode == RESULT_OK) {
            if (intent != null) {
                return new GooglePayPaymentAuthResult(PaymentData.getFromIntent(intent), null);
            }
        } else if (resultCode == RESULT_CANCELED) {
            return new GooglePayPaymentAuthResult(null, new UserCanceledException("User canceled Google Pay.", true));
        } else if (resultCode == RESULT_ERROR) {
            if (intent != null) {
                return new GooglePayPaymentAuthResult(null, new GooglePayException("An error was encountered during the Google Pay " +
                        "flow. See the status object in this exception for more details.",
                        AutoResolveHelper.getStatusFromIntent(intent)));
            }
        }
        return new GooglePayPaymentAuthResult(null, new BraintreeException("An unexpected error occurred."));
    }
}
