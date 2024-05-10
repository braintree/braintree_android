package com.braintreepayments.demo;

import android.content.Context;
import android.net.Uri;

import com.braintreepayments.api.BraintreeClient;

// TODO: remove when AuthorizationProvider is released
public class BraintreeClientFactory {

    static public BraintreeClient createBraintreeClientWithAuthorizationProvider(Context context, Uri appLinkUri) {
        return new BraintreeClient(context, new DemoClientTokenProvider(context), null, appLinkUri);
    }
}
