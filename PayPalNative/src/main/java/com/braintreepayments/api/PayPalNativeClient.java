package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

public class PayPalNativeClient {

    private final BraintreeClient braintreeClient;

    public PayPalNativeClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    public void tokenizePayPalAccount(final FragmentActivity activity) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(Configuration configuration, Exception error) {
                if (error != null) {
                    // TODO: callback error
                    return;
                }

                String payPalClientId = configuration.getPayPalClientId();
                if (payPalClientId == null) {
                    // TODO: callback error
                    return;
                }

                startPayPalNativeCheckout(activity, configuration, payPalClientId);
            }
        });
    }

    private void startPayPalNativeCheckout(FragmentActivity activity, Configuration configuration, String payPalClientId) {
        // TODO: configure and start paypal native checkout
    }
}
