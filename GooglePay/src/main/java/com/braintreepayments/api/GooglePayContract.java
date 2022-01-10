package com.braintreepayments.api;

import static com.braintreepayments.api.GooglePayClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.PaymentData;

public class GooglePayContract extends ActivityResultContract<GooglePayContractInput, GooglePayResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, GooglePayContractInput input) {
        return new Intent(context, GooglePayActivity.class)
                .putExtra(EXTRA_ENVIRONMENT, input.getEnvironment())
                .putExtra(EXTRA_PAYMENT_DATA_REQUEST, input.getPaymentDataRequest());
    }

    @Override
    public GooglePayResult parseResult(int resultCode, @Nullable Intent intent) {
        if (intent != null) {
            PaymentData paymentData = null;
            BraintreeException exception = null;

            if (resultCode == AppCompatActivity.RESULT_OK) {
                paymentData = PaymentData.getFromIntent(intent);
            } else if (resultCode == AutoResolveHelper.RESULT_ERROR) {
                exception = new GooglePayException("An error was encountered during the Google Pay " +
                        "flow. See the status object in this exception for more details.",
                                AutoResolveHelper.getStatusFromIntent(intent));
            } else if (resultCode == AppCompatActivity.RESULT_CANCELED) {
                exception = new UserCanceledException("User canceled Google Pay.");
            }
            return new GooglePayResult(paymentData, exception);
        }
        return null;
    }
}
