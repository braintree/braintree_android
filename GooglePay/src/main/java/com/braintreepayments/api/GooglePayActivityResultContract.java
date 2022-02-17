package com.braintreepayments.api;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.GooglePayClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST;
import static com.google.android.gms.wallet.AutoResolveHelper.RESULT_ERROR;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.PaymentData;

class GooglePayActivityResultContract extends ActivityResultContract<GooglePayIntentData, GooglePayResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, GooglePayIntentData input) {
        return new Intent(context, GooglePayActivity.class)
                .putExtra(EXTRA_ENVIRONMENT, input.getGooglePayEnvironment())
                .putExtra(EXTRA_PAYMENT_DATA_REQUEST, input.getPaymentDataRequest());
    }

    @Override
    public GooglePayResult parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode == RESULT_OK) {
            if (intent != null) {
                return new GooglePayResult(PaymentData.getFromIntent(intent), null);
            }
        } else if (resultCode == RESULT_CANCELED) {
            return new GooglePayResult(null, new UserCanceledException("User canceled Google Pay."));
        } else if (resultCode == RESULT_ERROR) {
            if (intent != null) {
                return new GooglePayResult(null, new GooglePayException("An error was encountered during the Google Pay " +
                        "flow. See the status object in this exception for more details.",
                        AutoResolveHelper.getStatusFromIntent(intent)));
            }
        }
        return new GooglePayResult(null, new BraintreeException("An unexpected error occurred."));
    }
}
