package com.braintreepayments.api;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class used to retrieve a customer's payment methods.
 */
public class PaymentMethodClient {

    private static final String PAYMENT_METHOD_NONCE_COLLECTION_KEY = "paymentMethods";

    private static final String SINGLE_USE_TOKEN_ID = "singleUseTokenId";
    private static final String VARIABLES = "variables";
    private static final String INPUT = "input";
    private static final String CLIENT_SDK_META_DATA = "clientSdkMetadata";

    private final BraintreeClient braintreeClient;

    public PaymentMethodClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Parses a response from the Braintree gateway for a list of payment method nonces.
     *
     * @param jsonBody Json-formatted String containing a list of {@link PaymentMethodNonce}s
     * @return List of {@link PaymentMethodNonce}s contained in jsonBody
     * @throws JSONException if parsing fails
     */
    static List<PaymentMethodNonce> parsePaymentMethodNonces(String jsonBody) throws JSONException {
        JSONArray paymentMethods =
            new JSONObject(jsonBody).getJSONArray(PAYMENT_METHOD_NONCE_COLLECTION_KEY);

        if (paymentMethods == null) {
            return Collections.emptyList();
        }

        List<PaymentMethodNonce> paymentMethodNonces = new ArrayList<>();
        JSONObject json;
        PaymentMethodNonce paymentMethodNonce;
        for(int i = 0; i < paymentMethods.length(); i++) {
            json = paymentMethods.getJSONObject(i);
            // TODO: leverage compileOnly gradle dependencies to return strongly-typed nonces (e.g. CardNonce, PayPalAccountNonce etc.)
            paymentMethodNonce = PaymentMethodNonce.fromJSON(json);
            if (paymentMethodNonce.getType() != PaymentMethodType.GOOGLE_PAY) {
                paymentMethodNonces.add(paymentMethodNonce);
            }
        }

        return paymentMethodNonces;
    }

    /**
     * Retrieves the current list of {@link PaymentMethodNonce}s for the current customer.
     * <p>
     * When finished, the {@link java.util.List} of {@link PaymentMethodNonce}s will be sent to {@link
     * GetPaymentMethodNoncesCallback}
     *  @param defaultFirst when {@code true} the customer's default payment method will be first in the list, otherwise
     *        payment methods will be ordered by most recently added.
     * @param callback {@link GetPaymentMethodNoncesCallback}
     */
    public void getPaymentMethodNonces(boolean defaultFirst, final GetPaymentMethodNoncesCallback callback) {
        final Uri uri = Uri.parse(TokenizationClient.versionedPath(TokenizationClient.PAYMENT_METHOD_ENDPOINT))
                .buildUpon()
                .appendQueryParameter("default_first", String.valueOf(defaultFirst))
                .appendQueryParameter("session_id", braintreeClient.getSessionId())
                .build();

        braintreeClient.sendGET(uri.toString(), new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    callback.onResult(parsePaymentMethodNonces(responseBody), null);
                    braintreeClient.sendAnalyticsEvent("get-payment-methods.succeeded");
                } catch (JSONException e) {
                    callback.onResult(null, e);
                    braintreeClient.sendAnalyticsEvent("get-payment-methods.failed");
                }
            }

            @Override
            public void failure(Exception exception) {
                callback.onResult(null, exception);
                braintreeClient.sendAnalyticsEvent("get-payment-methods.failed");
            }
        });
    }

    /**
     * Retrieves the current list of {@link PaymentMethodNonce}s for the current customer.
     * <p>
     * When finished, the {@link java.util.List} of {@link PaymentMethodNonce}s will be sent to {@link
     * GetPaymentMethodNoncesCallback}
     *
     * @param callback {@link GetPaymentMethodNoncesCallback}
     */
    public void getPaymentMethodNonces(GetPaymentMethodNoncesCallback callback) {
        getPaymentMethodNonces(false, callback);
    }

    /**
     * Deletes a payment method that belongs to the current customer.
     * used to instantiate the {@link BraintreeClient}.
     *
     * @param context Android Context
     * @param paymentMethodNonce The payment method nonce that references a vaulted payment method.
     * @param callback {@link DeletePaymentMethodNonceCallback}
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
                    .integration(braintreeClient.getIntegrationType())
                    .build());

            base.put(GraphQLConstants.Keys.QUERY, GraphQLQueryHelper.getQuery(
                    context, R.raw.delete_payment_method_mutation));
            input.put(SINGLE_USE_TOKEN_ID, paymentMethodNonce.getString());
            variables.put(INPUT, input);
            base.put(VARIABLES, variables);
            base.put(GraphQLConstants.Keys.OPERATION_NAME,
                    "DeletePaymentMethodFromSingleUseToken");
        } catch (Resources.NotFoundException | IOException | JSONException e) {
            Exception error = new BraintreeException("Unable to read GraphQL query");
            callback.onResult(null, error);
        }

        braintreeClient.sendGraphQLPOST(base.toString(), new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                callback.onResult(paymentMethodNonce, null);
                braintreeClient.sendAnalyticsEvent("delete-payment-methods.succeeded");
            }

            @Override
            public void failure(Exception exception) {
                Exception error = new PaymentMethodDeleteException(paymentMethodNonce, exception);
                callback.onResult(null, error);
                braintreeClient.sendAnalyticsEvent("delete-payment-methods.failed");
            }
        });
    }
}
