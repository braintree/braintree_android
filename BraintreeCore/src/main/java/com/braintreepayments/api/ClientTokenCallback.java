package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface ClientTokenCallback {
    void onSuccess(@NonNull String clientToken);
    void onFailure(@NonNull Exception exception);
}
