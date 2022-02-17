package com.braintreepayments.api;

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

//        Intent intent = new Intent(activity, GooglePayActivity.class)
//                .putExtra(EXTRA_ENVIRONMENT, getGooglePayEnvironment(configuration))
//                .putExtra(EXTRA_PAYMENT_DATA_REQUEST, paymentDataRequest);
        return null;
    }

    @Override
    public GooglePayResult parseResult(int resultCode, @Nullable Intent intent) {

        PaymentData paymentData = PaymentData.getFromIntent(intent);

        return null;
    }
}
