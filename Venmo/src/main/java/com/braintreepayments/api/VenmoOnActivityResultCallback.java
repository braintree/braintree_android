package com.braintreepayments.api;

import com.braintreepayments.api.models.VenmoAccountNonce;

public interface VenmoOnActivityResultCallback {

    void onResult(VenmoAccountNonce nonce, Exception error);
}
