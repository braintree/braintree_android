package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface ClientTokenProvider {
    void getClientToken(@NonNull ClientTokenCallback callback);
}
