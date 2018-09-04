package com.braintreepayments.api.models;

import android.content.Context;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.models.PayPalRequest.PayPalPaymentIntent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Builder used to construct a PayPal account tokenization request
 */
public class PayPalAccountBuilder extends PaymentMethodBuilder<PayPalAccountBuilder> {

    private static final String PAYPAL_ACCOUNT_KEY = "paypalAccount";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String INTENT_KEY = "intent";
    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchant_account_id";

    private String mClientMetadataId;
    private JSONObject mOneTouchCoreData = new JSONObject();
    private String mIntent;
    private String mMerchantAccountId;

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

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param intent Can be either {@link PayPalRequest#INTENT_AUTHORIZE} or {@link PayPalRequest#INTENT_SALE}.
     * @return {@link PayPalAccountBuilder}
     */
    public PayPalAccountBuilder intent(@PayPalPaymentIntent String intent) {
        mIntent = intent;
        return this;
    }

    /**
     * Used to set a non-default merchant account id.
     *
     * @param merchantAccountId String merchant account id
     * @return {@link PayPalAccountBuilder}
     */
    public PayPalAccountBuilder merchantAccountId(String merchantAccountId) {
        mMerchantAccountId = merchantAccountId;
        return this;
    }

    @Override
    protected void build(JSONObject base, JSONObject paymentMethodNonceJson) throws JSONException {
        paymentMethodNonceJson.put(CORRELATION_ID_KEY, mClientMetadataId);
        paymentMethodNonceJson.put(INTENT_KEY, mIntent);

        Iterator<String> otcKeyIterator = mOneTouchCoreData.keys();
        while (otcKeyIterator.hasNext()) {
            String otcKey = otcKeyIterator.next();
            paymentMethodNonceJson.put(otcKey, mOneTouchCoreData.get(otcKey));
        }

        if(mMerchantAccountId != null) {
            base.put(MERCHANT_ACCOUNT_ID_KEY, mMerchantAccountId);
        }

        base.put(PAYPAL_ACCOUNT_KEY, paymentMethodNonceJson);
    }

    @Override
    protected void buildGraphQL(Context context, JSONObject base, JSONObject input)
            throws BraintreeException, JSONException {}

    @Override
    public String getApiPath() {
        return "paypal_accounts";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return PayPalAccountNonce.TYPE;
    }
}
