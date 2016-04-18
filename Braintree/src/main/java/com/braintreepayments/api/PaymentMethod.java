package com.braintreepayments.api;

import android.net.Uri;

import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;

import java.util.List;

/**
 * Class used to retrieve a customer's payment methods.
 */
public class PaymentMethod {

    /**
     * Retrieves the current list of {@link PaymentMethodNonce}s for the current customer.
     * <p/>
     * When finished, the {@link java.util.List} of {@link PaymentMethodNonce}s will be sent to {@link
     * PaymentMethodNoncesUpdatedListener#onPaymentMethodNoncesUpdated(List)}.
     *
     * @param fragment {@link BraintreeFragment}
     * @param defaultFirst when {@code true} the customer's default payment method will be first in the list, otherwise
     *        payment methods will be ordered my most recently used.
     */
    public static void getPaymentMethodNonces(final BraintreeFragment fragment, boolean defaultFirst) {
        final Uri uri = Uri.parse(TokenizationClient.versionedPath(TokenizationClient.PAYMENT_METHOD_ENDPOINT))
                .buildUpon()
                .appendQueryParameter("default_first", String.valueOf(defaultFirst))
                .build();

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fragment.getHttpClient().get(uri.toString(), new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        try {
                            fragment.postCallback(PaymentMethodNonce.parsePaymentMethodNonces(responseBody));
                        } catch (JSONException e) {
                            fragment.postCallback(e);
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        fragment.postCallback(exception);
                    }
                });
            }
        });
    }

    /**
     * Retrieves the current list of {@link PaymentMethodNonce}s for the current customer.
     * <p/>
     * When finished, the {@link java.util.List} of {@link PaymentMethodNonce}s will be sent to {@link
     * PaymentMethodNoncesUpdatedListener#onPaymentMethodNoncesUpdated(List)}.
     *
     * @param fragment {@link BraintreeFragment}
     */
    public static void getPaymentMethodNonces(BraintreeFragment fragment) {
        getPaymentMethodNonces(fragment, false);
    }
}
