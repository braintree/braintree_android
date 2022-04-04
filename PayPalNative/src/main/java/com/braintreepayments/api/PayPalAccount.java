package com.braintreepayments.api;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Use to construct a PayPal account tokenization request
 */
class PayPalAccount extends PaymentMethod {

    private static final String PAYPAL_ACCOUNT_KEY = "paypalAccount";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String INTENT_KEY = "intent";
    private static final String MERCHANT_ACCOUNT_ID_KEY = "merchant_account_id";

    private String clientMetadataId;
    private JSONObject urlResponseData = new JSONObject();
    private String intent;
    private String merchantAccountId;
    private String paymentType;

    PayPalAccount() {
        super();
    }

    @Override
    JSONObject buildJSON() throws JSONException {
        JSONObject json = super.buildJSON();

        JSONObject paymentMethodNonceJson = new JSONObject();
        paymentMethodNonceJson.put(CORRELATION_ID_KEY, clientMetadataId);
        paymentMethodNonceJson.put(INTENT_KEY, intent);

        if ("single-payment".equalsIgnoreCase(paymentType)) {
            JSONObject optionsJson = new JSONObject();
            optionsJson.put(VALIDATE_KEY, false);
            paymentMethodNonceJson.put(OPTIONS_KEY, optionsJson);
        }

        Iterator<String> urlResponseDataKeyIterator = urlResponseData.keys();
        while (urlResponseDataKeyIterator.hasNext()) {
            String key = urlResponseDataKeyIterator.next();
            paymentMethodNonceJson.put(key, urlResponseData.get(key));
        }

        if (merchantAccountId != null) {
            json.put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId);
        }
        json.put(PAYPAL_ACCOUNT_KEY, paymentMethodNonceJson);
        return json;
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param clientMetadataId Application clientMetadataId created by
     *                         {@link com.braintreepayments.api.PayPalDataCollector#getClientMetadataId(Context)}.
     */
    void setClientMetadataId(String clientMetadataId) {
        this.clientMetadataId = clientMetadataId;
    }

    /**
     * Response data from callback url. Used by PayPal wrappers to construct
     * a request to create a PayPal account.
     * <p>
     * Response data will be merged into the payment method json on {@link #buildJSON()}
     *
     * @param urlResponseData The data parsed from the PayPal callback url.
     */
    void setUrlResponseData(JSONObject urlResponseData) {
        if (urlResponseData != null) {
            this.urlResponseData = urlResponseData;
        }
    }

    /**
     * Used by PayPal wrappers to construct a request to create a PayPal account.
     *
     * @param intent Can be either {@link PayPalPaymentIntent#AUTHORIZE} or {@link PayPalPaymentIntent#SALE}.
     */
    void setIntent(@PayPalPaymentIntent String intent) {
        this.intent = intent;
    }

    /**
     * Used to set a non-default merchant account id.
     *
     * @param merchantAccountId String merchant account id
     */
    void setMerchantAccountId(String merchantAccountId) {
        this.merchantAccountId = merchantAccountId;
    }

    /**
     * Payment type from original PayPal request.
     *
     * @param paymentType Either "billing-agreement" or "single-payment"
     */
    void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    @Override
    String getApiPath() {
        return "paypal_accounts";
    }
}
