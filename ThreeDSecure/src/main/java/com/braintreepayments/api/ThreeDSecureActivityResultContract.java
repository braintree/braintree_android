package com.braintreepayments.api;

import static com.braintreepayments.api.ThreeDSecureActivity.EXTRA_ERROR_MESSAGE;
import static com.braintreepayments.api.ThreeDSecureActivity.EXTRA_JWT;
import static com.braintreepayments.api.ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT;
import static com.braintreepayments.api.ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

class ThreeDSecureActivityResultContract
        extends ActivityResultContract<ThreeDSecureResult, ThreeDSecurePaymentAuthResult> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, ThreeDSecureResult input) {
        Intent intent = new Intent(context, ThreeDSecureActivity.class);

        Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_THREE_D_SECURE_RESULT, input);
        intent.putExtras(extras);
        return intent;
    }

    @Override
    public ThreeDSecurePaymentAuthResult parseResult(int resultCode, @Nullable Intent intent) {
        ThreeDSecurePaymentAuthResult result;

        if (resultCode == Activity.RESULT_CANCELED) {
            result = new ThreeDSecurePaymentAuthResult(new UserCanceledException("User canceled 3DS."));
        } else if (intent == null) {
            String unknownErrorMessage =
                    "An unknown Android error occurred with the activity result API.";
            result = new ThreeDSecurePaymentAuthResult(new BraintreeException(unknownErrorMessage));
        } else if (resultCode == ThreeDSecureActivity.RESULT_COULD_NOT_START_CARDINAL) {
            String errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE);
            result = new ThreeDSecurePaymentAuthResult(new BraintreeException(errorMessage));
        } else {
            ThreeDSecureResult paymentAuthRequest =
                    intent.getParcelableExtra(EXTRA_THREE_D_SECURE_RESULT);
            ValidateResponse validateResponse =
                    (ValidateResponse) intent.getSerializableExtra(EXTRA_VALIDATION_RESPONSE);
            String jwt = intent.getStringExtra(EXTRA_JWT);
            result = new ThreeDSecurePaymentAuthResult(paymentAuthRequest, jwt, validateResponse);
        }
        return result;
    }
}
