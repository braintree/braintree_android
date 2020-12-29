package com.braintreepayments.api;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.PaymentMethodDeleteException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNoncesUpdatedListener;
import com.braintreepayments.api.internal.GraphQLConstants;
import com.braintreepayments.api.internal.GraphQLQueryHelper;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.MetadataBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Class used to retrieve a customer's payment methods.
 */
public class PaymentMethodClient {

    protected static final String SINGLE_USE_TOKEN_ID = "singleUseTokenId";
    protected static final String VARIABLES = "variables";
    protected static final String INPUT = "input";
    protected static final String CLIENT_SDK_META_DATA = "clientSdkMetadata";

    private BraintreeClient braintreeClient;

    PaymentMethodClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Retrieves the current list of {@link PaymentMethodNonce}s for the current customer.
     * <p>
     * When finished, the {@link java.util.List} of {@link PaymentMethodNonce}s will be sent to {@link
     * PaymentMethodNoncesUpdatedListener#onPaymentMethodNoncesUpdated(List)}.
     *
     * @param fragment {@link BraintreeFragment}
     * @param defaultFirst when {@code true} the customer's default payment method will be first in the list, otherwise
     *        payment methods will be ordered my most recently used.
     */
    public void getPaymentMethodNonces(final Context context, boolean defaultFirst, final GetPaymentMethodNoncesCallback callback) {
        final Uri uri = Uri.parse(TokenizationClient.versionedPath(TokenizationClient.PAYMENT_METHOD_ENDPOINT))
                .buildUpon()
                .appendQueryParameter("default_first", String.valueOf(defaultFirst))
                .appendQueryParameter("session_id", braintreeClient.getSessionId())
                .build();

        braintreeClient.sendGET(uri.toString(), context, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    callback.onResult(PaymentMethodNonce.parsePaymentMethodNonces(responseBody), null);
                    braintreeClient.sendAnalyticsEvent(context, "get-payment-methods.succeeded");
                } catch (JSONException e) {
                    callback.onResult(null, e);
                    braintreeClient.sendAnalyticsEvent(context, "get-payment-methods.failed");
                }
            }

            @Override
            public void failure(Exception exception) {
                callback.onResult(null, exception);
                braintreeClient.sendAnalyticsEvent(context, "get-payment-methods.failed");
            }
        });
    }

    /**
     * Retrieves the current list of {@link PaymentMethodNonce}s for the current customer.
     * <p>
     * When finished, the {@link java.util.List} of {@link PaymentMethodNonce}s will be sent to {@link
     * PaymentMethodNoncesUpdatedListener#onPaymentMethodNoncesUpdated(List)}.
     *
     * @param fragment {@link BraintreeFragment}
     */
    public void getPaymentMethodNonces(Context context, GetPaymentMethodNoncesCallback callback) {
        getPaymentMethodNonces(context, false, callback);
    }

    /**
     * Deletes a payment method owned by the customer whose id was used to generate the {@link ClientToken}
     * used to create the {@link BraintreeFragment}.
     * <p>
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
    // TODO: Investigate if this feature should be removed from Android or added to iOS for feature parity
    public void deletePaymentMethod(final Context context, final PaymentMethodNonce paymentMethodNonce, final DeletePaymentMethodNonceCallback callback) {
        boolean usesClientToken = braintreeClient.getAuthorization() instanceof ClientToken;

        if (!usesClientToken) {
            Exception error =
                new BraintreeException("A client token with a customer id must be used to delete a payment method nonce.");
            callback.onResult(null, error);
            return;
        }

        final JSONObject base = new JSONObject();
        JSONObject variables = new JSONObject();
        JSONObject input = new JSONObject();

        try {
            base.put(CLIENT_SDK_META_DATA, new MetadataBuilder()
                    .sessionId(braintreeClient.getSessionId())
                    .source("client")
                    .integration(braintreeClient.getIntegrationType(context))
                    .build());

            base.put(GraphQLConstants.Keys.QUERY, GraphQLQueryHelper.getQuery(
                    context, R.raw.delete_payment_method_mutation));
            input.put(SINGLE_USE_TOKEN_ID, paymentMethodNonce.getNonce());
            variables.put(INPUT, input);
            base.put(VARIABLES, variables);
            base.put(GraphQLConstants.Keys.OPERATION_NAME,
                    "DeletePaymentMethodFromSingleUseToken");
        } catch (Resources.NotFoundException | IOException | JSONException e) {
            Exception error = new BraintreeException("Unable to read GraphQL query");
            callback.onResult(null, error);
        }

        braintreeClient.sendGraphQLPOST(base.toString(), context, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                callback.onResult(paymentMethodNonce, null);
                braintreeClient.sendAnalyticsEvent(context, "delete-payment-methods.succeeded");
            }

            @Override
            public void failure(Exception exception) {
                Exception error = new PaymentMethodDeleteException(paymentMethodNonce, exception);
                callback.onResult(null, error);
                braintreeClient.sendAnalyticsEvent(context, "delete-payment-methods.failed");
            }
        });
    }
}
