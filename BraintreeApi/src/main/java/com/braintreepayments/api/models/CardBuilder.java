package com.braintreepayments.api.models;

import com.braintreepayments.api.models.Card.BillingAddress;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder used to construct a {@link com.braintreepayments.api.models.Card}
 * @see com.braintreepayments.api.models.Card
 * @see com.braintreepayments.api.models.PaymentMethod.Builder
 */
public class CardBuilder implements PaymentMethod.Builder<Card> {

    @SerializedName("creditCard") private Card mCard;

    public CardBuilder() {
        mCard = new Card();
    }

    /**
     * @param number The card number.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder cardNumber(String number) {
        mCard.setCardNumber(number);
        return this;
    }

    /**
     * @param cvv The card's CVV.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder cvv(String cvv) {
        mCard.setCvv(cvv);
        return this;
    }

    /**
     * @param expirationMonth The expiration month of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder expirationMonth(String expirationMonth) {
        mCard.setExpirationMonth(expirationMonth);
        return this;
    }

    /**
     * @param expirationYear The expiration year of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder expirationYear(String expirationYear) {
        mCard.setExpirationYear(expirationYear);
        return this;
    }

    /**
     * @param expirationDate The expiration date of the card.
     *                       May be in the form MM/YY or MM/YYYY.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder expirationDate(String expirationDate) {
        mCard.setExpirationDate(expirationDate);
        return this;
    }

    /**
     * @param postalCode Postal code of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder postalCode(String postalCode) {
        BillingAddress billingAddress = new BillingAddress();
        billingAddress.setPostalCode(postalCode);
        mCard.setBillingAddress(billingAddress);
        return this;
    }

    @Override
    public CardBuilder validate(boolean validate) {
        PaymentMethodOptions options = new PaymentMethodOptions();
        options.setValidate(validate);
        mCard.setOptions(options);
        return this;
    }

    @Override
    public Card build() {
        return mCard;
    }

    @Override
    public Map<String, Object> toJson() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("creditCard", build());
        return params;
    }

    @Override
    public Card fromJson(String json) {
        return Card.fromJson(json);
    }

    @Override
    public String getApiPath() {
        return "credit_cards";
    }

    @Override
    public String getApiResource() {
        return "creditCards";
    }
}
