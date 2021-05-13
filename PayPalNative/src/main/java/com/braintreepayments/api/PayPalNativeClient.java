package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONObject;

/**
 * Used to tokenize PayPal accounts using PayPal Native UI. For more information see the
 * <a href="https://developers.braintreepayments.com/guides/paypal/overview/android/">documentation</a>
 */
public class PayPalNativeClient {

    private final BraintreeClient braintreeClient;
    private final PayPalInternalClient internalPayPalClient;
    private PayPalClient payPalClient;

    public PayPalNativeClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
        this.internalPayPalClient = new PayPalInternalClient(braintreeClient);
    }

    public void tokenizePayPalAccount(final FragmentActivity activity, final PayPalNativeRequest request, final PayPalNativeFlowStartedCallback callback) {
        if (request instanceof PayPalNativeCheckoutRequest) {
            sendNativeCheckoutRequest(activity, (PayPalNativeCheckoutRequest) request, callback);
        } else if (request instanceof PayPalNativeVaultRequest) {
            sendVaultRequest(activity, (PayPalNativeVaultRequest) request, callback);
        }
    }

    private void sendNativeCheckoutRequest(final FragmentActivity activity, final PayPalNativeCheckoutRequest request, final PayPalNativeFlowStartedCallback callback) {
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
                startPayPalNativeCheckout(activity, configuration, payPalClientId, request, callback);
            }
        });
    }

    private void sendVaultRequest(final FragmentActivity activity, final PayPalNativeVaultRequest payPalVaultRequest, final PayPalNativeFlowStartedCallback callback) {
        //this one should default to the one we already have, once PayPalNative supports billing agreements, this should just default to native.
        payPalClient = new PayPalClient(braintreeClient);
        payPalClient.tokenizePayPalAccount(activity, payPalVaultRequest, new PayPalFlowStartedCallback() {
            @Override
            public void onResult(@Nullable Exception error) {
                callback.onResult(error);
            }
        });
    }

    private void startPayPalNativeCheckout(final FragmentActivity activity, Configuration configuration, String payPalClientId, final PayPalNativeCheckoutRequest payPalRequest, final PayPalNativeFlowStartedCallback callback) {
        // TODO: configure paypal native checkout

        internalPayPalClient.sendRequest(activity, payPalRequest, new PayPalInternalClientCallback() {
            @Override
            public void onResult(PayPalResponse payPalResponse, Exception error) {
                if (payPalResponse != null) {
                    // TODO: start native checkout
                    callback.onResult(new BraintreeException("TODO: integrate native checkout with Braintree SDK."));
                } else {
                    callback.onResult(error);
                }
            }
        });
    }

    private static Exception createPayPalNotEnabledError() {
        return new BraintreeException("PayPal is not enabled. " +
                "See https://developers.braintreepayments.com/guides/paypal/overview/android/ " +
                "for more information.");
    }

    public void onActivityResumed(final BrowserSwitchResult browserSwitchResult, final PayPalNativeOnActivityResumedCallback callback) {
        if (browserSwitchResult == null) {
            callback.onResult(null, new BraintreeException("BrowserSwitchResult cannot be null"));
            return;
        }
        JSONObject metadata = browserSwitchResult.getRequestMetadata();
        String paymentType = Json.optString(metadata, "payment-type", "unknown");
        boolean isVaultPayment = paymentType.equalsIgnoreCase("billing-agreement");

        if (isVaultPayment) {
            //it means it was a vault request
            payPalClient.onBrowserSwitchResult(browserSwitchResult, new PayPalBrowserSwitchResultCallback() {
                @Override
                public void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error) {
                    callback.onResult(payPalAccountNonce, error);
                }
            });
        }
    }
}
