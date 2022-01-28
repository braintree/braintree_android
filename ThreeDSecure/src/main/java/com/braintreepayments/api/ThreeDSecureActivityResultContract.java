package com.braintreepayments.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

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
        CardinalResult result;
        if (resultCode != Activity.RESULT_OK) {
            Exception userCanceledError = new UserCanceledException("User canceled 3DS.");
            result = new CardinalResult(userCanceledError);
        } else {
            ThreeDSecureResult threeDSecureResult =
                    intent.getParcelableExtra(ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT);
            ValidateResponse validateResponse =
                    (ValidateResponse) intent.getSerializableExtra(ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE);
            String jwt = intent.getStringExtra(ThreeDSecureActivity.EXTRA_JWT);
            return new CardinalResult(threeDSecureResult, jwt, validateResponse);
        }
        return result;
    }
}
