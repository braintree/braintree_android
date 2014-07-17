package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.api.Utils;

import java.io.Serializable;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a PayPal account.
 * @see {@link com.braintreepayments.api.models.Card}
 * @see {@link com.braintreepayments.api.models.PaymentMethod}
 */
public class PayPalAccount extends PaymentMethod implements Serializable {

    protected static final String PAYMENT_METHOD_TYPE = "PayPalAccount";

    private String consentCode;
    private String correlationId;
    private PayPalDetails details;

    protected void setEmail(String email) {
        details = new PayPalDetails();
        details.setEmail(email);
    }

    protected void setConsentCode(String consentCode) {
        this.consentCode = consentCode;
    }

    protected void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * @return The email address associated with this PayPal account
     */
    public String getEmail() {
        if (details != null) {
            return details.getEmail();
        } else {
            return "";
        }
    }

    /**
     * @return The description of this PayPal account for displaying to a customer, either email or
     * 'PayPal'
     */
    @Override
    public String getDescription() {
        if (TextUtils.equals(super.getDescription(), "PayPal") && !TextUtils.isEmpty(getEmail())) {
            return getEmail();
        } else {
            return super.getDescription();
        }
    }

    /**
     * @return The type of this {@link com.braintreepayments.api.models.PaymentMethod} (always "PayPal")
     */
    @Override
    public String getTypeLabel() {
        return "PayPal";
    }

    /**
     * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
     * @param json Raw JSON representation of a {@link com.braintreepayments.api.models.PayPalAccount}.
     * @return {@link com.braintreepayments.api.models.PayPalAccount} for use in payment method selection UIs.
     */
    public static PayPalAccount fromJson(String json) {
        return Utils.getGson().fromJson(json, PayPalAccount.class);
    }

    private class PayPalDetails implements Serializable {
        private String email;

        private String getEmail() {
            return email;
        }

        private void setEmail(String email) {
            this.email = email;
        }
    }
}
