package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class ThreeDSecureActivityResultContract extends ActivityResultContract<ThreeDSecureResult, CardinalResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, ThreeDSecureResult input) {
        Intent intent = new Intent(context, ThreeDSecureActivity.class);

        Bundle extras = new Bundle();
        extras.putParcelable(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT, input);
        intent.putExtras(extras);
        return intent;
    }

    @Override
    public CardinalResult parseResult(int resultCode, @Nullable Intent intent) {
        return null;
    }
}
