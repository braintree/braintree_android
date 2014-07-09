package com.braintreepayments.api.models;

import com.braintreepayments.api.Utils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * {@link com.braintreepayments.api.models.PaymentMethod} representing a credit or debit card.
 * @see com.braintreepayments.api.models.PaymentMethod
 * @see com.braintreepayments.api.models.PayPalAccount
 */
public class Card extends PaymentMethod implements Serializable {

    protected static final String PAYMENT_METHOD_TYPE = "CreditCard";

    private BillingAddress billingAddress;
    private CardDetails details;
    private String expirationMonth;
    private String expirationYear;
    private String expirationDate;
    @SerializedName("number") private String cardNumber;
    private String cvv;

    /**
     * @return Type of this card (e.g. MasterCard, American Express)
     */
    @Override
    public String getTypeLabel() {
        return details.getCardType();
    }

    /**
     * @return Last two digits of the card, intended for display purposes.
     */
    public String getLastTwo() {
        return details.getLastTwo();
    }

    protected void setBillingAddress(BillingAddress billingAddress) {
        this.billingAddress = billingAddress;
    }

    protected void setExpirationMonth(String expirationMonth) {
        this.expirationMonth = expirationMonth;
    }

    protected void setExpirationYear(String expirationYear) {
        this.expirationYear = expirationYear;
    }

    protected void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    protected void setCardNumber(String number) {
        this.cardNumber = number;
    }

    protected void setCvv(String cvv) {
        this.cvv = cvv;
    }

    /**
     * Required for and handled by {@link com.braintreepayments.api.Braintree}. Not intended for general consumption.
     * @param creditCard Raw JSON representation of a {@link com.braintreepayments.api.models.Card}.
     * @return {@link com.braintreepayments.api.models.Card} for use in payment method selection UIs.
     */
    public static Card fromJson(String creditCard) {
        return Utils.getGson().fromJson(creditCard, Card.class);
    }

    private class CardDetails implements Serializable {
        private String cardType;
        private String lastTwo;

        protected String getCardType() { return cardType; }
        protected String getLastTwo() { return lastTwo; }
    }

    protected static class BillingAddress implements Serializable {
        private String postalCode;

        protected void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
    }
}
