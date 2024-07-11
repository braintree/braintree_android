package com.braintreepayments.api.threedsecure;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.core.BraintreeException;
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver;
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse;

/**
 * The Activity that receives Cardinal SDK result from 3DS v2 flow
 */
public class ThreeDSecureActivity extends AppCompatActivity {

    static final String EXTRA_ERROR_MESSAGE =
            "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_ERROR_MESSAGE";
    static final String EXTRA_THREE_D_SECURE_RESULT =
            "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_THREE_D_SECURE_RESULT";
    static final String EXTRA_VALIDATION_RESPONSE =
            "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_VALIDATION_RESPONSE";
    static final String EXTRA_JWT = "com.braintreepayments.api.ThreeDSecureActivity.EXTRA_JWT";

    static final int RESULT_COULD_NOT_START_CARDINAL = RESULT_FIRST_USER;

    private final CardinalClient cardinalClient = new CardinalClient();
    private CardinalChallengeObserver challengeObserver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        challengeObserver = new CardinalChallengeObserver(
                this, (context, validateResponse, s) -> handleValidated(cardinalClient, validateResponse, s));

        /*
            Here, we schedule the 3DS auth challenge launch to run immediately after onCreate() is
            complete. This gives the CardinalValidateReceiver callback a chance to run before 3DS
            is initiated.
         */
        new Handler(Looper.getMainLooper()).post(() -> launchCardinalAuthChallenge(cardinalClient));
    }

    @VisibleForTesting
    void launchCardinalAuthChallenge(CardinalClient cardinalClient) {
        if (isFinishing()) {
            /*
               After a process kill, we may have already parsed a Cardinal result via the
               CardinalChallengeObserver and are in the process of propagating the result back to
               the merchant app.

               This guard prevents the Cardinal Auth Challenge from being shown while the Activity
               is finishing.
             */
            return;
        }

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            extras = new Bundle();
        }

        ThreeDSecureParams threeDSecureParams = extras.getParcelable(EXTRA_THREE_D_SECURE_RESULT);
        if (threeDSecureParams != null) {
            try {
                cardinalClient.continueLookup(threeDSecureParams, challengeObserver);
            } catch (BraintreeException e) {
                finishWithError(e.getMessage());
            }
        } else {
            finishWithError("Unable to launch 3DS authentication.");
        }
    }

    private void finishWithError(String errorMessage) {
        Intent result = new Intent();
        result.putExtra(EXTRA_ERROR_MESSAGE, errorMessage);
        setResult(RESULT_COULD_NOT_START_CARDINAL, result);
        finish();
    }

    @VisibleForTesting
    void handleValidated(CardinalClient cardinalClient, ValidateResponse validateResponse, String jwt) {
        cardinalClient.cleanup();

        Intent result = new Intent();
        result.putExtra(EXTRA_JWT, jwt);
        result.putExtra(EXTRA_THREE_D_SECURE_RESULT, (ThreeDSecureParams) getIntent().getExtras()
                .getParcelable(EXTRA_THREE_D_SECURE_RESULT));
        result.putExtra(EXTRA_VALIDATION_RESPONSE, validateResponse);

        setResult(RESULT_OK, result);
        finish();
    }
}
