package com.braintreepayments.api;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodResponseCallback;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethod;

public class CardTokenizer {

    /**
     * Create a {@link com.braintreepayments.api.models.Card} in the Braintree Gateway.
     *
     * On completion, returns the {@link PaymentMethod} to
     * {@link com.braintreepayments.api.interfaces.PaymentMethodCreatedListener}.
     *
     * If creation fails validation, {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onRecoverableError(ErrorWithResponse)}
     * will be called with the resulting {@link ErrorWithResponse}.
     *
     * If an error not due to validation (server error, network issue, etc.) occurs,
     * {@link com.braintreepayments.api.interfaces.BraintreeErrorListener#onUnrecoverableError(Throwable)} will be called
     * with the {@link Exception} that occurred.
     *
     * @param cardBuilder {@link CardBuilder}
     */
    public static void tokenize(final BraintreeFragment fragment, final CardBuilder cardBuilder) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched() {
                PaymentMethodTokenization.tokenize(fragment, cardBuilder,
                        new PaymentMethodResponseCallback() {
                            @Override
                            public void success(PaymentMethod paymentMethod) {
                                fragment.postCallback(paymentMethod);
                                fragment.sendAnalyticsEvent("card.nonce-received");
                            }

                            @Override
                            public void failure(Exception exception) {
                                fragment.postCallback(exception);
                                fragment.sendAnalyticsEvent("card.nonce-failed");
                            }
                        });
            }
        });
    }
}
