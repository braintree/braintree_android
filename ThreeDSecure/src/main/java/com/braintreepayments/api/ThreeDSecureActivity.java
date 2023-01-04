package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver;

/**
 * The Activity that receives Cardinal SDK result from 3DS v2 flow
 */
public class ThreeDSecureActivity extends AppCompatActivity implements CardinalValidateReceiver {

    static final String EXTRA_ERROR_MESSAGE = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_ERROR_MESSAGE";
    static final String EXTRA_THREE_D_SECURE_RESULT = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT";
    static final String EXTRA_VALIDATION_RESPONSE = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE";
    static final String EXTRA_JWT = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_JWT";

    private final CardinalClient cardinalClient = new CardinalClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateInternal(cardinalClient);
    }

    @VisibleForTesting
    void onCreateInternal(CardinalClient cardinalClient) {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            extras = new Bundle();
        }

        ThreeDSecureResult threeDSecureResult = extras.getParcelable(EXTRA_THREE_D_SECURE_RESULT);
        if (threeDSecureResult != null) {
            cardinalClient.continueLookup(this, threeDSecureResult, this);
        } else {
            Intent result = new Intent();
            result.putExtra(EXTRA_ERROR_MESSAGE, "Unable to launch 3DS authentication.");

            setResult(RESULT_CANCELED, result);
            finish();
        }
    }

    @Override
    public void onValidated(Context context, ValidateResponse validateResponse, String jwt) {
        Intent result = new Intent();
        result.putExtra(EXTRA_JWT, jwt);
        result.putExtra(EXTRA_THREE_D_SECURE_RESULT, (ThreeDSecureResult) getIntent().getExtras()
                .getParcelable(EXTRA_THREE_D_SECURE_RESULT));
        result.putExtra(EXTRA_VALIDATION_RESPONSE, validateResponse);

        setResult(RESULT_OK, result);
        finish();
    }
}
