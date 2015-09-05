package com.braintreepayments.api.models;

import android.content.Context;

import com.braintreepayments.api.models.PaymentMethod.Builder;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder used to construct a {@link com.braintreepayments.api.models.PayPalAccount}
 * @see com.braintreepayments.api.models.PayPalAccount
 * @see com.braintreepayments.api.models.PaymentMethod.Builder
 */
public class PayPalAccountBuilder implements PaymentMethod.Builder<PayPalAccount> {

    @SerializedName("email") private String mEmail;
    @SerializedName("authorizationCode") private String mAuthorizationCode;
    @SerializedName("correlationId") private String mCorrelationId;
    @SerializedName("options") private PaymentMethodOptions mPaymentMethodOptions;
    private String mIntegration = "custom";
    private String mSource;

    /**
     * Used by PayPal wrappers to construct a {@link com.braintreepayments.api.models.PayPalAccount}.
     * @param email address to display to the user
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder}
     */
    public PayPalAccountBuilder email(String email) {
        mEmail = email;
        return this;
    }

    /**
     * Used by PayPal wrappers to construct a {@link com.braintreepayments.api.models.PayPalAccount}.
     * @param authorizationCode Authorization code returned by PayPal SDK.
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder}
     */
    public PayPalAccountBuilder authorizationCode(String authorizationCode) {
        mAuthorizationCode = authorizationCode;
        return this;
    }

    /**
     * Used by PayPal wrappers to construct a {@link com.braintreepayments.api.models.PayPalAccount}.
     * @param correlationId Application correlation ID created by
     * {@link com.paypal.android.sdk.payments.PayPalConfiguration#getClientMetadataId(Context)}
     * to verify the payment.
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder}
     */
    public PayPalAccountBuilder correlationId(String correlationId) {
        mCorrelationId = correlationId;
        return this;
    }

    @Override
    public PayPalAccountBuilder validate(boolean validate) {
        mPaymentMethodOptions = new PaymentMethodOptions();
        mPaymentMethodOptions.setValidate(validate);
        return this;
    }

    @Override
    public PayPalAccountBuilder integration(String integration) {
        mIntegration = integration;
        return this;
    }

    @Override
    public PayPalAccountBuilder source(String source) {
        mSource = source;
        return this;
    }

    @Override
    public PayPalAccount build() {
        PayPalAccount payPalAccount = new PayPalAccount();
        payPalAccount.setConsentCode(mAuthorizationCode);
        payPalAccount.setCorrelationId(mCorrelationId);
        payPalAccount.setOptions(mPaymentMethodOptions);
        payPalAccount.setSource(mSource);

        return payPalAccount;
    }

    @Override
    public Map<String, Object> toJson() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("paypalAccount", build());
        params.put(Builder.METADATA_KEY, new Metadata(mIntegration, mSource));
        return params;
    }

    @Override
    public String toJsonString() {
        return new Gson().toJson(toJson());
    }

    @Override
    public PayPalAccount fromJson(String json) {
        PayPalAccount payPalAccount = PayPalAccount.fromJson(json);
        payPalAccount.setEmail(mEmail);

        return payPalAccount;
    }

    @Override
    public String getApiPath() {
        return "paypal_accounts";
    }

    @Override
    public String getApiResource() {
        return "paypalAccounts";
    }
}
