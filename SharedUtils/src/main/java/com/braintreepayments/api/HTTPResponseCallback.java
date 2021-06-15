package com.braintreepayments.api;

import androidx.annotation.MainThread;

interface HTTPResponseCallback {

    @MainThread
    void onResult(String responseBody, Exception httpError);
}
