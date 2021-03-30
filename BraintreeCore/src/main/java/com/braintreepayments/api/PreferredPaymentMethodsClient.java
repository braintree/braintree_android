package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Fetches information about which payment methods are preferred on the device.
 * Used to determine which payment methods are given preference in your UI,
 * not whether they are presented entirely.
 * This class is currently in beta and may change in future releases.
 */
public class PreferredPaymentMethodsClient {

    private final BraintreeClient braintreeClient;
    private final DeviceInspector deviceInspector;

    public PreferredPaymentMethodsClient(BraintreeClient braintreeClient) {
        this(braintreeClient, new DeviceInspector());
    }

    @VisibleForTesting
    PreferredPaymentMethodsClient(BraintreeClient braintreeClient, DeviceInspector deviceInspector) {
        this.braintreeClient = braintreeClient;
        this.deviceInspector = deviceInspector;
    }

    /**
     * Fetches information about which payment methods should be given preference in your UI.
     *
     * @param context Android Context
     * @param callback {@link PreferredPaymentMethodsCallback}
     */
    public void fetchPreferredPaymentMethods(final Context context, final PreferredPaymentMethodsCallback callback) {

        final Context applicationContext = context.getApplicationContext();
        final boolean isVenmoAppInstalled = deviceInspector.isVenmoInstalled(applicationContext);
        final boolean isPayPalAppInstalled = deviceInspector.isPayPalInstalled(applicationContext);

        final String venmoAppInstalledEvent =
            String.format("preferred-payment-methods.venmo.app-installed.%b", isVenmoAppInstalled);
        braintreeClient.sendAnalyticsEvent(venmoAppInstalledEvent);

        if (isPayPalAppInstalled) {
            braintreeClient.sendAnalyticsEvent("preferred-payment-methods.paypal.app-installed.true");
            callback.onResult(new PreferredPaymentMethodsResult()
                    .isPayPalPreferred(true)
                    .isVenmoPreferred(isVenmoAppInstalled));
            return;
        }

        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                boolean isGraphQLDisabled = (configuration == null || !configuration.isGraphQLEnabled());
                if (isGraphQLDisabled) {
                    braintreeClient.sendAnalyticsEvent("preferred-payment-methods.api-disabled");
                    callback.onResult(new PreferredPaymentMethodsResult()
                            .isPayPalPreferred(isPayPalAppInstalled)
                            .isVenmoPreferred(isVenmoAppInstalled));
                    return;
                }

                final String query = "{ \"query\": \"query PreferredPaymentMethods { preferredPaymentMethods { paypalPreferred } }\" }";

                braintreeClient.sendGraphQLPOST(query, new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        PreferredPaymentMethodsResult result =
                                PreferredPaymentMethodsResult.fromJSON(responseBody, isVenmoAppInstalled);

                        String payPalPreferredEvent =
                                String.format("preferred-payment-methods.paypal.api-detected.%b", result.isPayPalPreferred());
                        braintreeClient.sendAnalyticsEvent(payPalPreferredEvent);

                        callback.onResult(result);
                    }

                    @Override
                    public void failure(Exception exception) {
                        braintreeClient.sendAnalyticsEvent("preferred-payment-methods.api-error");
                        callback.onResult(new PreferredPaymentMethodsResult()
                                .isPayPalPreferred(false)
                                .isVenmoPreferred(isVenmoAppInstalled));
                    }
                });
            }
        });
    }
}
