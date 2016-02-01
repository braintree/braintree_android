package com.braintreepayments.api.models;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Builder used to construct a PayPal account tokenization request
 */
public class PayPalAccountBuilder extends PaymentMethodBuilder<PayPalAccountBuilder> {

    private static final String PAYPAL_ACCOUNT_KEY = "paypalAccount";
    private static final String CORRELATION_ID_KEY = "correlationId";

    public PayPalAccountBuilder() {
        super();

        try {
            mJson.put(PAYPAL_ACCOUNT_KEY, mPaymentMethodNonceJson);
        } catch (JSONException ignored) {}
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param clientMetadataId Application clientMetadataId created by
     * {@link com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore#getClientMetadataId(Context)}.
     * @return {@link PayPalAccountBuilder}
     */
    public PayPalAccountBuilder clientMetadataId(String clientMetadataId) {
        try {
            mPaymentMethodNonceJson.put(CORRELATION_ID_KEY, clientMetadataId);
        } catch (JSONException ignored) {}

        return this;
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @note Merge the OTC data into the payment method json.
     * @param otcData The data provided by OneTouchCore.
     * @return {@link PayPalAccountBuilder}
     */
    public PayPalAccountBuilder oneTouchCoreData(JSONObject otcData) {
        try {
            Iterator<String> otcKeyIterator = otcData.keys();
            while(otcKeyIterator.hasNext()){
                String otcKey = otcKeyIterator.next();
                mPaymentMethodNonceJson.put(otcKey, otcData.get(otcKey));
            }
        } catch (JSONException ignored) {}
        return this;
    }

    @Override
    public String getApiPath() {
        return "paypal_accounts";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return PayPalAccountNonce.TYPE;
    }
}
