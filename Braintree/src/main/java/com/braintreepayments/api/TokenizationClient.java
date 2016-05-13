package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;

import org.json.JSONException;

import static com.braintreepayments.api.models.PaymentMethodNonce.parsePaymentMethodNonces;

class TokenizationClient {

    static final String PAYMENT_METHOD_ENDPOINT = "payment_methods";

    /**
     * Create a {@link PaymentMethodNonce} in the Braintree Gateway.
     * <p/>
     * On completion, returns the {@link PaymentMethodNonce} to {@link PaymentMethodNonceCallback}.
     * <p/>
     * If creation fails validation, {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)}
     * will be called with the resulting {@link ErrorWithResponse}.
     * <p/>
     * If an error not due to validation (server error, network issue, etc.) occurs, {@link
     * com.braintreepayments.api.interfaces.BraintreeErrorListener#onError(Exception)} (Throwable)}
     * will be called with the {@link Exception} that occurred.
     *
     * @param paymentMethodBuilder {@link PaymentMethodBuilder} for the {@link PaymentMethodNonce}
     *        to be created.
     */
    static void tokenize(final BraintreeFragment fragment, final PaymentMethodBuilder paymentMethodBuilder,
            final PaymentMethodNonceCallback callback) {
        paymentMethodBuilder.setSessionId(fragment.getSessionId());

        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                fragment.getHttpClient().post(TokenizationClient.versionedPath(
                        TokenizationClient.PAYMENT_METHOD_ENDPOINT + "/" + paymentMethodBuilder.getApiPath()),
                        paymentMethodBuilder.build(), new HttpResponseCallback() {
                            @Override
                            public void success(String responseBody) {
                                try {
                                    PaymentMethodNonce paymentMethodNonce = parsePaymentMethodNonces(responseBody,
                                            paymentMethodBuilder.getResponsePaymentMethodType());
                                    callback.success(paymentMethodNonce);
                                } catch (JSONException e) {
                                    callback.failure(e);
                                }
                            }

                            @Override
                            public void failure(Exception exception) {
                                callback.failure(exception);
                            }
                        });
            }
        });
    }

    static String versionedPath(String path) {
        return "/v1/" + path;
    }
}
