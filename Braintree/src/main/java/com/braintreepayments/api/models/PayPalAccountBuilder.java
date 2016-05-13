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

    private String mClientMetadataId;
    private JSONObject mOneTouchCoreData = new JSONObject();

    public PayPalAccountBuilder() {
        super();
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param clientMetadataId Application clientMetadataId created by
     * {@link com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore#getClientMetadataId(Context)}.
     * @return {@link PayPalAccountBuilder}
     */
    public PayPalAccountBuilder clientMetadataId(String clientMetadataId) {
        mClientMetadataId = clientMetadataId;
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
        if (otcData != null) {
            mOneTouchCoreData = otcData;
        }
        return this;
    }

    @Override
    protected void build(JSONObject base, JSONObject paymentMethodNonceJson) throws JSONException {
        paymentMethodNonceJson.put(CORRELATION_ID_KEY, mClientMetadataId);

        Iterator<String> otcKeyIterator = mOneTouchCoreData.keys();
        while (otcKeyIterator.hasNext()) {
            String otcKey = otcKeyIterator.next();
            paymentMethodNonceJson.put(otcKey, mOneTouchCoreData.get(otcKey));
        }

        base.put(PAYPAL_ACCOUNT_KEY, paymentMethodNonceJson);
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
