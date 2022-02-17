package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class GooglePayActivityResultContract extends ActivityResultContract<GooglePayIntentData, GooglePayResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, GooglePayIntentData input) {
        return null;
    }

    @Override
    public GooglePayResult parseResult(int resultCode, @Nullable Intent intent) {
        return null;
    }
}
