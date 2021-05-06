package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

public class PayPalNativeClient {

    private final BraintreeClient braintreeClient;

    public PayPalNativeClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    public void tokenizePayPalAccount(final FragmentActivity activity, final PayPalNativeFlowStartedCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(Configuration configuration, Exception error) {
                if (error != null) {
                    callback.onResult(createPayPalNotEnabledError());
                    return;
                }

                String payPalClientId = configuration.getPayPalClientId();
                if (payPalClientId == null) {
                    callback.onResult(new BraintreeException("PayPal Client ID is null. TODO: write error message explaining why PayPal Native couldn't be started"));
                    return;
                }

                // NOTE: the callback parameter is only necessary if PayPal Native XO needs
                // to callback an error before starting the native UI
                startPayPalNativeCheckout(activity, configuration, payPalClientId, callback);
            }
        });
    }

    private void startPayPalNativeCheckout(FragmentActivity activity, Configuration configuration, String payPalClientId, PayPalNativeFlowStartedCallback callback) {
        // TODO: configure and start paypal native checkout
        callback.onResult(new BraintreeException("TODO: integrate native checkout with Braintree SDK."));
    }

    private static Exception createPayPalNotEnabledError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.");
    }
}
