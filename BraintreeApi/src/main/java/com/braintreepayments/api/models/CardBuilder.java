package com.braintreepayments.api.models;

import com.braintreepayments.api.Utils;
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
    private BillingAddress mBillingAddress = null;
    private String mIntegration = "custom";
    private String mSource = "form";

    public CardBuilder() {
        mCard = new Card();
        mCard.setSource(mSource);
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

    private BillingAddress getBillingAddress() {
        if (mBillingAddress == null) {
            mBillingAddress = new BillingAddress();
            mCard.setBillingAddress(mBillingAddress);
        }
        return mBillingAddress;
    }

    /**
     * @param firstName First name on the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder firstName(String firstName) {
        getBillingAddress().setFirstName(firstName);
        return this;
    }

    /**
     * @param lastName Last name on the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder lastName(String lastName) {
        getBillingAddress().setLastName(lastName);
        return this;
    }

    /**
     * @param countryName Country name of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder countryName(String countryName) {
        getBillingAddress().setCountryName(countryName);
        return this;
    }

    /**
     * @param locality Locality of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder locality(String locality) {
        getBillingAddress().setLocality(locality);
        return this;
    }

    /**
     * @param postalCode Postal code of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder postalCode(String postalCode) {
        getBillingAddress().setPostalCode(postalCode);
        return this;
    }

    /**
     * @param region Region of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder region(String region) {
        getBillingAddress().setRegion(region);
        return this;
    }

    /**
     * @param streetAddress Street address of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder streetAddress(String streetAddress) {
        getBillingAddress().setStreetAddress(streetAddress);
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
    public CardBuilder integration(String integration) {
        mIntegration = integration;
        return this;
    }

    @Override
    public CardBuilder source(String source) {
        mSource = source;
        mCard.setSource(mSource);
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
        params.put(PaymentMethod.Builder.METADATA_KEY, new Metadata(mIntegration, mSource));
        return params;
    }

    @Override
    public String toJsonString() {
        return Utils.getGson().toJson(toJson());
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