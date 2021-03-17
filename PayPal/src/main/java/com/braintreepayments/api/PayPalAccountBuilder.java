package com.braintreepayments.api;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Builder used to construct a PayPal account tokenization request
 */
class PayPalAccountBuilder extends PaymentMethodBuilder<PayPalAccountBuilder> {

    private static final String PAYPAL_ACCOUNT_KEY = "paypalAccount";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String INTENT_KEY = "intent";
    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchant_account_id";

    private String mClientMetadataId;
    private JSONObject mUrlResponseData = new JSONObject();
    private String mIntent;
    private String mMerchantAccountId;

    PayPalAccountBuilder() {
        super();
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param clientMetadataId Application clientMetadataId created by
     * {@link com.braintreepayments.api.PayPalDataCollector#getClientMetadataId(Context)}.
     * @return {@link PayPalAccountBuilder}
     */
    PayPalAccountBuilder clientMetadataId(String clientMetadataId) {
        mClientMetadataId = clientMetadataId;
        return this;
    }

    /**
     * Response data from callback url. Used by PayPal wrappers to construct
     * a request to create a PayPal account.
     *
     * Response data will be merged into the payment method json on {@link #build()}
     *
     * @param urlResponseData The data parsed from the PayPal callback url.
     * @return {@link PayPalAccountBuilder}
     */
    PayPalAccountBuilder urlResponseData(JSONObject urlResponseData) {
        if (urlResponseData != null) {
            mUrlResponseData = urlResponseData;
        }
        return this;
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param intent Can be either {@link PayPalCheckoutRequest#INTENT_AUTHORIZE} or {@link PayPalCheckoutRequest#INTENT_SALE}.
     * @return {@link PayPalAccountBuilder}
     */
    PayPalAccountBuilder intent(@PayPalCheckoutRequest.PayPalPaymentIntent String intent) {
        mIntent = intent;
        return this;
    }

    /**
     * Used to set a non-default merchant account id.
     *
     * @param merchantAccountId String merchant account id
     * @return {@link PayPalAccountBuilder}
     */
    PayPalAccountBuilder merchantAccountId(String merchantAccountId) {
        mMerchantAccountId = merchantAccountId;
        return this;
    }

    @Override
    protected void build(JSONObject base, JSONObject paymentMethodNonceJson) throws JSONException {
        paymentMethodNonceJson.put(CORRELATION_ID_KEY, mClientMetadataId);
        paymentMethodNonceJson.put(INTENT_KEY, mIntent);

        Iterator<String> urlResponseDataKeyIterator = mUrlResponseData.keys();
        while (urlResponseDataKeyIterator.hasNext()) {
            String key = urlResponseDataKeyIterator.next();
            paymentMethodNonceJson.put(key, mUrlResponseData.get(key));
        }

        if(mMerchantAccountId != null) {
            base.put(MERCHANT_ACCOUNT_ID_KEY, mMerchantAccountId);
        }

        base.put(PAYPAL_ACCOUNT_KEY, paymentMethodNonceJson);
    }

    @Override
    protected void buildGraphQL(JSONObject base, JSONObject input) {}

    @Override
    public String getApiPath() {
        return "paypal_accounts";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return PayPalAccountNonce.TYPE;
    }
}
