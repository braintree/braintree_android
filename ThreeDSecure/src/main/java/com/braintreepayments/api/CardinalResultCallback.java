package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Callback for receiving the results via {@link ThreeDSecureLauncher}
 */
public interface CardinalResultCallback {

    void onCardinalResult(@NonNull CardinalResult cardinalResult);
}
