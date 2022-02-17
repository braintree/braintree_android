package com.braintreepayments.api;

import static com.braintreepayments.api.GooglePayClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

        PaymentData paymentData = PaymentData.getFromIntent(intent);

        return null;
    }
}
