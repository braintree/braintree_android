package com.braintreepayments.api;

import android.content.Context;

// TODO: remove when AuthorizationProvider is released
public class BraintreeClientFactory {

    static public BraintreeClient createBraintreeClientWithAuthorizationProvider(Context context) {
        return new BraintreeClient(context, new DemoAuthorizationProvider(context));
    }
}
