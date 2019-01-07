package com.braintreepayments.api;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.PaymentMethodDeleteException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.internal.GraphQLConstants;
import com.braintreepayments.api.internal.GraphQLQueryHelper;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.MetadataBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Class used to retrieve a customer's payment methods.
 */
public class PaymentMethod {

    protected static final String SINGLE_USE_TOKEN_ID = "singleUseTokenId";
    protected static final String VARIABLES = "variables";
    protected static final String INPUT = "input";
    protected static final String CLIENT_SDK_META_DATA = "clientSdkMetadata";

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
                .appendQueryParameter("session_id", fragment.getSessionId())
                .build();

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fragment.getHttpClient().get(uri.toString(), new HttpResponseCallback() {
                    @Override
                    public void success(String responseBody) {
                        try {
                            fragment.postCallback(PaymentMethodNonce.parsePaymentMethodNonces(responseBody));
                            fragment.sendAnalyticsEvent("get-payment-methods.succeeded");
                        } catch (JSONException e) {
                            fragment.postCallback(e);
                            fragment.sendAnalyticsEvent("get-payment-methods.failed");
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        fragment.postCallback(exception);
                        fragment.sendAnalyticsEvent("get-payment-methods.failed");
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

    /**
     * Deletes a payment method owned by the customer whose id was used to generate the {@link ClientToken}
     * used to create the {@link BraintreeFragment}.
     * <p/>
     * Note: This method only works with Android Lollipop (>= 21) and above.
     * This will invoke {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)} when
     * <ul>
     *      <li>A {@link com.braintreepayments.api.models.TokenizationKey} is used.</li>
     *      <li>The device is below Lollipop.</li>
     *      <li>If the request fails.</li>
     * <ul/>
     *
     * @param fragment {@link BraintreeFragment}
     * @param paymentMethodNonce The payment method nonce that references a vaulted payment method.
     */
    public static void deletePaymentMethod(final BraintreeFragment fragment,
                                           final PaymentMethodNonce paymentMethodNonce)  {

        boolean usesClientToken = fragment.getAuthorization() instanceof ClientToken;

        if (!usesClientToken) {
            fragment.postCallback(new BraintreeException("A client token with a customer id must be used to delete a payment method nonce."));
            return;
        }

        JSONObject base = new JSONObject();
        JSONObject variables = new JSONObject();
        JSONObject input = new JSONObject();

        try {
            base.put(CLIENT_SDK_META_DATA, new MetadataBuilder()
                    .sessionId(fragment.getSessionId())
                    .source("client")
                    .integration(fragment.getIntegrationType())
                    .build());

            base.put(GraphQLConstants.Keys.QUERY, GraphQLQueryHelper.getQuery(
                    fragment.getApplicationContext(), R.raw.delete_payment_method_mutation));
            input.put(SINGLE_USE_TOKEN_ID, paymentMethodNonce.getNonce());
            variables.put(INPUT, input);
            base.put(VARIABLES, variables);
            base.put(GraphQLConstants.Keys.OPERATION_NAME,
                    "DeletePaymentMethodFromSingleUseToken");
        } catch (Resources.NotFoundException | IOException | JSONException e) {
            fragment.postCallback(new BraintreeException("Unable to read GraphQL query"));
        }

        fragment.getGraphQLHttpClient().post(base.toString(), new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fragment.postPaymentMethodDeletedCallback(paymentMethodNonce);
                fragment.sendAnalyticsEvent("delete-payment-methods.succeeded");
            }

            @Override
            public void failure(Exception exception) {
                fragment.postCallback(new PaymentMethodDeleteException(paymentMethodNonce, exception));
                fragment.sendAnalyticsEvent("delete-payment-methods.failed");
            }
        });
    }
}
