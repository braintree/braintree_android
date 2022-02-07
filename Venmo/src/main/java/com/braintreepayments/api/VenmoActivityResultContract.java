package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class VenmoActivityResultContract extends ActivityResultContract<VenmoIntentData, VenmoResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, VenmoIntentData input) {
        return null;
    }

    @Override
    public VenmoResult parseResult(int resultCode, @Nullable Intent intent) {
        return null;
    }
}
