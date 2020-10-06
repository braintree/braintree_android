package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ThreeDSecureActivity extends AppCompatActivity implements CardinalValidateReceiver {

    static final String EXTRA_THREE_D_SECURE_LOOKUP = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP";
    static final String EXTRA_VALIDATION_RESPONSE = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE";
    static final String EXTRA_JWT = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_JWT";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        ThreeDSecureLookup threeDSecureLookup = extras.getParcelable(EXTRA_THREE_D_SECURE_LOOKUP);

        Cardinal.getInstance().cca_continue(
                threeDSecureLookup.getTransactionId(),
                threeDSecureLookup.getPareq(),
                this,
                this
        );
    }

    @Override
    public void onValidated(Context context, ValidateResponse validateResponse, String jwt) {
        Intent result = new Intent();
        result.putExtra(EXTRA_JWT, jwt);
        result.putExtra(EXTRA_THREE_D_SECURE_LOOKUP, getIntent().getExtras()
                .getParcelable(EXTRA_THREE_D_SECURE_LOOKUP));
        result.putExtra(EXTRA_VALIDATION_RESPONSE, validateResponse);

        setResult(RESULT_OK, result);
        finish();
    }
}
