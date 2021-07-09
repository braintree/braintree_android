package com.braintreepayments.api;

import androidx.annotation.MainThread;

interface HttpResponseCallback {

    @MainThread
    void onResult(String responseBody, Exception httpError);
}
