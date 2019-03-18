package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.cardinalcommerce.cardinalmobilesdk.Cardinal;
import com.cardinalcommerce.cardinalmobilesdk.models.response.ValidateResponse;
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver;
import com.cardinalcommerce.shared.models.enums.DirectoryServerID;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ThreeDSecureActivity extends AppCompatActivity implements CardinalValidateReceiver {

    static final String EXTRA_THREE_D_SECURE_LOOKUP = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_THREE_D_SECURE_LOOKUP";
    static final String EXTRA_TRANSACTION_ID = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_TRANSACTION_ID";
    static final String EXTRA_PAREQ = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_PAREQ";
    static final String EXTRA_ACS_URL = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_ACS_URL";
    static final String EXTRA_VALIDATION_RESPONSE = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE";
    static final String EXTRA_JWT = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_JWT";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        String transactionId = extras.getString(EXTRA_TRANSACTION_ID);
        String pareq = extras.getString(EXTRA_PAREQ);
        String acsUrl = extras.getString(EXTRA_ACS_URL);

        Cardinal.getInstance().cca_continue(
                transactionId,
                pareq,
                acsUrl,
                DirectoryServerID.VISA01,
                this,
                fragment,
                this
        );
    }

    @Override
    public void onValidated(Context context, ValidateResponse validateResponse, String jwt) {
        Intent result = new Intent();
        result.putExtra(EXTRA_VALIDATION_RESPONSE, validateResponse);
        result.putExtra(EXTRA_JWT, jwt);
        result.putExtra(EXTRA_THREE_D_SECURE_LOOKUP, getIntent().getExtras()
                .getParcelable(EXTRA_THREE_D_SECURE_LOOKUP));

        setResult(RESULT_OK, result);
        finish();
    }
}
