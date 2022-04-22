package com.braintreepayments.demo.factories;

import android.content.Context;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.demo.DemoClientTokenProvider;
import com.braintreepayments.demo.Settings;

public class BraintreeClientFactory {

    private BraintreeClientFactory() {
    }

    public static BraintreeClient createBraintreeClient(Context context) {
        if (Settings.useTokenizationKey(context)) {
            return new BraintreeClient(context, Settings.getTokenizationKey(context));
        } else {
            return new BraintreeClient(context, new DemoClientTokenProvider(context));
        }
    }
}
