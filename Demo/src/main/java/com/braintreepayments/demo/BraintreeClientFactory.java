package com.braintreepayments.demo;

import android.content.Context;

import com.braintreepayments.api.BraintreeClient;

// TODO: remove when AuthorizationProvider is released
public class BraintreeClientFactory {

    static public BraintreeClient createBraintreeClientWithAuthorizationProvider(Context context) {
        return new BraintreeClient(context, new DemoClientTokenProvider(context));
    }
}
