package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public class PayPalLauncher {

    private final BrowserSwitchClient browserSwitchClient;

    public PayPalLauncher() {
        browserSwitchClient = new BrowserSwitchClient();
    }
    public void launch(@NonNull FragmentActivity activity, PayPalResponse payPalResponse)
            throws BrowserSwitchException {
        browserSwitchClient.start(activity, payPalResponse.getBrowserSwitchOptions());
    }

    public BrowserSwitchResult deliverResult(Context context, Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(context, PAYPAL, intent);
        browserSwitchClient.clearActiveRequests(context);
        return result;
    }
}
