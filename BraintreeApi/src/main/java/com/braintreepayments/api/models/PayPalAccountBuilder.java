package com.braintreepayments.api.models;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.paypal.android.sdk.onetouch.core.Result;

import org.json.JSONException;
import org.json.JSONObject;

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
    private JSONObject mOtcResponse;

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
     *
     * @param correlationId Application correlation ID created by
     * {@link com.paypal.android.sdk.payments.PayPalConfiguration#getClientMetadataId(Context)}
     * to verify the payment.
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder}
     */
    public PayPalAccountBuilder correlationId(String correlationId) {
        this.mCorrelationId = correlationId;
        return this;
    }

    public PayPalAccountBuilder OtcResponse(JSONObject OtcResponse) {
        mOtcResponse = OtcResponse;
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
    public String toJsonString() {
        JSONObject params = new JSONObject();
        try {
            Boolean isValidate = mPaymentMethodOptions != null && mPaymentMethodOptions.isValidate();
            mOtcResponse.put("options", new JSONObject().put("validate", isValidate));
            params.put("paypal_account", mOtcResponse);
            params.put("correlation_id", mCorrelationId);
            params.put(PaymentMethod.Builder.METADATA_KEY, new JSONObject(new Gson()
                    .toJson(new Metadata(mIntegration, mSource))));
        } catch (JSONException ignored) {}
        return params.toString();
    }

    @Override
    public PayPalAccount fromJson(String json) {
        PayPalAccount payPalAccount = PayPalAccount.fromJson(json);
        if (payPalAccount.getEmail() == null) {
            payPalAccount.setEmail(mEmail);
        }
        return payPalAccount;
    }

    @Override
    public String getApiPath() { return "paypal_accounts"; }

    @Override
    public String getApiResource() {
        return "paypalAccounts";
    }
}
