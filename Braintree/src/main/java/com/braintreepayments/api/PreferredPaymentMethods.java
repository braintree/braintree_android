package com.braintreepayments.api;

import android.content.pm.PackageManager;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PreferredPaymentMethodsListener;
import com.braintreepayments.api.internal.BraintreeGraphQLHttpClient;
import com.braintreepayments.api.models.PreferredPaymentMethodsResult;

/**
 * Fetches information about which payment methods are preferred on the device.
 * Used to determine which payment methods are given preference in your UI,
 * not whether they are presented entirely.
 * This class is currently in beta and may change in future releases.
 */
public class PreferredPaymentMethods {

    private static final String PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile";
    private static final int NO_FLAGS = 0;

    /**
     * Fetches information about which payment methods should be given preference in your UI.
     * @param fragment The BraintreeFragment
     * @param listener A listener that is invoked when preferred payment methods have been fetched.
     */
    public static void fetchPreferredPaymentMethods(final BraintreeFragment fragment, final PreferredPaymentMethodsListener listener) {

        boolean isPayPalAppInstalled = false;
        try {
            PackageManager packageManager = fragment.getApplicationContext().getPackageManager();
            isPayPalAppInstalled = packageManager.getApplicationInfo(PAYPAL_APP_PACKAGE, NO_FLAGS) != null;
        } catch (PackageManager.NameNotFoundException ignored) {
            // do nothing
        }

        if (isPayPalAppInstalled) {
            fragment.sendAnalyticsEvent("preferred-payment-methods.paypal.app-installed.true");
            listener.onPreferredPaymentMethodsFetched(new PreferredPaymentMethodsResult().isPayPalPreferred(true));
            return;
        }

        BraintreeGraphQLHttpClient graphQLClient = fragment.getGraphQLHttpClient();
        if (graphQLClient == null) {
            fragment.sendAnalyticsEvent("preferred-payment-methods.api-disabled");
            listener.onPreferredPaymentMethodsFetched(new PreferredPaymentMethodsResult().isPayPalPreferred(false));
            return;
        }

        String query = "{ \"query\": \"query ClientConfiguration { clientConfiguration { paypal { preferredPaymentMethod } } }\" }";
        graphQLClient.post(query, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                PreferredPaymentMethodsResult preferredPaymentMethodsResult = PreferredPaymentMethodsResult.fromJSON(responseBody);
                fragment.sendAnalyticsEvent(String.format("preferred-payment-methods.paypal.api-detected.%b", preferredPaymentMethodsResult.isPayPalPreferred()));
                listener.onPreferredPaymentMethodsFetched(preferredPaymentMethodsResult);
            }

            @Override
            public void failure(Exception exception) {
                fragment.sendAnalyticsEvent("preferred-payment-methods.api-error");
                listener.onPreferredPaymentMethodsFetched(new PreferredPaymentMethodsResult().isPayPalPreferred(false));
            }
        });
    }
}
