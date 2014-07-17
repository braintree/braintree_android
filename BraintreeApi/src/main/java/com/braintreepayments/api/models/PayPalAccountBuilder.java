package com.braintreepayments.api.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder used to construct a {@link com.braintreepayments.api.models.PayPalAccount}
 * @see com.braintreepayments.api.models.PayPalAccount
 * @see com.braintreepayments.api.models.PaymentMethod.Builder
 */
public class PayPalAccountBuilder implements PaymentMethod.Builder<PayPalAccount> {

    private String email;
    private String authorizationCode;
    private String correlationId;
    private PaymentMethodOptions options;

    /**
     * Used by PayPal wrappers to construct a {@link com.braintreepayments.api.models.PayPalAccount}.
     * @param email address to display to the user
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder}
     */
    public PayPalAccountBuilder email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Used by PayPal wrappers to construct a {@link com.braintreepayments.api.models.PayPalAccount}.
     * @param authorizationCode Authorization code returned by PayPal SDK.
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder}
     */
    public PayPalAccountBuilder authorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
        return this;
    }

    /**
     * Used by PayPal wrappers to construct a {@link com.braintreepayments.api.models.PayPalAccount}.
     * @param correlationId Application correlation ID created by
     * {@link com.paypal.android.sdk.payments.PayPalConfiguration#getApplicationCorrelationId(android.app.Activity)}
     * to verify the payment.
     * @return {@link com.braintreepayments.api.models.PayPalAccountBuilder}
     */
    public PayPalAccountBuilder correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    @Override
    public PayPalAccount build() {
        PayPalAccount payPalAccount = new PayPalAccount();
        payPalAccount.setConsentCode(authorizationCode);
        payPalAccount.setCorrelationId(correlationId);
        payPalAccount.setOptions(options);

        return payPalAccount;
    }

    @Override
    public PayPalAccountBuilder validate(boolean validate) {
        options = new PaymentMethodOptions();
        options.setValidate(validate);
        return this;
    }

    @Override
    public Map<String, Object> toJson() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("paypalAccount", build());
        return params;
    }

    @Override
    public PayPalAccount fromJson(String json) {
        PayPalAccount payPalAccount = PayPalAccount.fromJson(json);
        payPalAccount.setEmail(email);

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
