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

    private static final int NO_FLAGS = 0;
    private static final String PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile";
    private static final String VENMO_APP_PACKAGE = "com.venmo";

    /**
     * Fetches information about which payment methods should be given preference in your UI.
     *
     * @param fragment The BraintreeFragment
     * @param listener A listener that is invoked when preferred payment methods have been fetched.
     */
    public static void fetchPreferredPaymentMethods(final BraintreeFragment fragment, final PreferredPaymentMethodsListener listener) {

        boolean isPayPalAppInstalled = false;
        boolean isVenmoAppInstalled = false;
        try {
            PackageManager packageManager = fragment.getApplicationContext().getPackageManager();
            isPayPalAppInstalled = packageManager.getApplicationInfo(PAYPAL_APP_PACKAGE, NO_FLAGS) != null;
            isVenmoAppInstalled = Venmo.isVenmoInstalled(fragment.getApplicationContext());
        } catch (PackageManager.NameNotFoundException ignored) {
            // do nothing
        }

        fragment.sendAnalyticsEvent(String.format("preferred-payment-methods.venmo.app-installed.%b", isVenmoAppInstalled));

        if (isPayPalAppInstalled) {
            fragment.sendAnalyticsEvent("preferred-payment-methods.paypal.app-installed.true");
            listener.onPreferredPaymentMethodsFetched(new PreferredPaymentMethodsResult()
                    .isPayPalPreferred(true)
                    .isVenmoPreferred(isVenmoAppInstalled));
            return;
        }

        BraintreeGraphQLHttpClient graphQLClient = fragment.getGraphQLHttpClient();
        if (graphQLClient == null) {
            fragment.sendAnalyticsEvent("preferred-payment-methods.api-disabled");
            listener.onPreferredPaymentMethodsFetched(new PreferredPaymentMethodsResult()
                    .isPayPalPreferred(isPayPalAppInstalled)
                    .isVenmoPreferred(isVenmoAppInstalled));
            return;
        }

        String query = "{ \"query\": \"query PreferredPaymentMethods { preferredPaymentMethods { paypalPreferred } }\" }";
        final boolean finalIsVenmoAppInstalled = isVenmoAppInstalled;

        graphQLClient.post(query, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON(responseBody, finalIsVenmoAppInstalled);

                fragment.sendAnalyticsEvent(String.format("preferred-payment-methods.paypal.api-detected.%b", result.isPayPalPreferred()));
                listener.onPreferredPaymentMethodsFetched(result);
            }

            @Override
            public void failure(Exception exception) {
                fragment.sendAnalyticsEvent("preferred-payment-methods.api-error");
                listener.onPreferredPaymentMethodsFetched(new PreferredPaymentMethodsResult()
                        .isPayPalPreferred(false)
                        .isVenmoPreferred(finalIsVenmoAppInstalled));
            }
        });
    }
}
