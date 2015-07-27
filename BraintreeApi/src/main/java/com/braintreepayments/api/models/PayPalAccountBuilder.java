package com.braintreepayments.api.models;

import android.content.Context;

import org.json.JSONException;

/**
 * Builder used to construct a PayPal account tokenization request
 */
public class PayPalAccountBuilder extends PaymentMethodBuilder<PayPalAccountBuilder> {

    private static final String PAYPAL_ACCOUNT_KEY = "paypalAccount";
    private static final String CONSENT_CODE_KEY = "consentCode";
    private static final String CORRELATION_ID_KEY = "correlationId";

    public PayPalAccountBuilder() {
        super();

        try {
            mJson.put(PAYPAL_ACCOUNT_KEY, mPaymentMethodJson);
        } catch (JSONException ignored) {}
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param consentCode consent code returned by PayPal SDK.
     * @return {@link PayPalAccountBuilder}
     */
    public PayPalAccountBuilder consentCode(String consentCode) {
        try {
            mPaymentMethodJson.put(CONSENT_CODE_KEY, consentCode);
        } catch (JSONException ignored) {}

        return this;
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param correlationId Application correlation ID created by
     * {@link com.paypal.android.sdk.payments.PayPalConfiguration#getClientMetadataId(Context)}
     * to verify the payment.
     * @return {@link PayPalAccountBuilder}
     */
    public PayPalAccountBuilder correlationId(String correlationId) {
        try {
            mPaymentMethodJson.put(CORRELATION_ID_KEY, correlationId);
        } catch (JSONException ignored) {}

        return this;
    }

    @Override
    public String getApiPath() {
        return "paypal_accounts";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return PayPalAccount.PAYMENT_METHOD_TYPE;
    }
}
