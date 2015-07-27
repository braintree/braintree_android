package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builder used to construct a card tokenization request.
 */
public class CardBuilder extends PaymentMethodBuilder<CardBuilder> {

    private static final String CREDIT_CARD_KEY = "creditCard";
    private static final String NUMBER_KEY = "number";
    private static final String CVV_KEY = "cvv";
    private static final String EXPIRATION_MONTH_KEY = "expirationMonth";
    private static final String EXPIRATION_YEAR_KEY = "expirationYear";
    private static final String EXPIRATION_DATE_KEY = "expirationDate";
    private static final String BILLING_ADDRESS_KEY = "billingAddress";
    private static final String FIRST_NAME_KEY = "firstName";
    private static final String LAST_NAME_KEY = "lastName";
    private static final String COUNTRY_NAME_KEY = "countryName";
    private static final String LOCALITY_KEY = "locality";
    private static final String POSTAL_CODE_KEY = "postalCode";
    private static final String REGION_KEY = "region";
    private static final String STREET_ADDRESS_KEY = "streetAddress";

    private final JSONObject mBillingAddressJsonObject;

    public CardBuilder() {
        super();

        mBillingAddressJsonObject = new JSONObject();

        try {
            mJson.put(CREDIT_CARD_KEY, mPaymentMethodJson);
        } catch (JSONException ignored) {}
    }

    /**
     * @param number The card number.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder cardNumber(String number) {
        try {
            mPaymentMethodJson.put(NUMBER_KEY, number);
        } catch (JSONException ignored) {}

        return this;
    }

    /**
     * @param cvv The card's CVV.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder cvv(String cvv) {
        try {
            mPaymentMethodJson.put(CVV_KEY, cvv);
        } catch (JSONException ignored) {}

        return this;
    }

    /**
     * @param expirationMonth The expiration month of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder expirationMonth(String expirationMonth) {
        try {
            mPaymentMethodJson.put(EXPIRATION_MONTH_KEY, expirationMonth);
        } catch (JSONException ignored) {}

        return this;
    }

    /**
     * @param expirationYear The expiration year of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder expirationYear(String expirationYear) {
        try {
            mPaymentMethodJson.put(EXPIRATION_YEAR_KEY, expirationYear);
        } catch (JSONException ignored) {}

        return this;
    }

    /**
     * @param expirationDate The expiration date of the card.
     *                       May be in the form MM/YY or MM/YYYY.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder expirationDate(String expirationDate) {
        try {
            mPaymentMethodJson.put(EXPIRATION_DATE_KEY, expirationDate);
        } catch (JSONException ignored) {}

        return this;
    }

    /**
     * @param firstName First name on the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder firstName(String firstName) {
        try {
            mBillingAddressJsonObject.put(FIRST_NAME_KEY, firstName);
            mPaymentMethodJson.put(BILLING_ADDRESS_KEY, mBillingAddressJsonObject);
        } catch (JSONException ignored) {}
        return this;
    }

    /**
     * @param lastName Last name on the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder lastName(String lastName) {
        try {
            mBillingAddressJsonObject.put(LAST_NAME_KEY, lastName);
            mPaymentMethodJson.put(BILLING_ADDRESS_KEY, mBillingAddressJsonObject);
        } catch (JSONException ignored) {}
        return this;
    }

    /**
     * @param countryName Country name of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder countryName(String countryName) {
        try {
            mBillingAddressJsonObject.put(COUNTRY_NAME_KEY, countryName);
            mPaymentMethodJson.put(BILLING_ADDRESS_KEY, mBillingAddressJsonObject);
        } catch (JSONException ignored) {}
        return this;
    }

    /**
     * @param locality Locality of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder locality(String locality) {
        try {
            mBillingAddressJsonObject.put(LOCALITY_KEY, locality);
            mPaymentMethodJson.put(BILLING_ADDRESS_KEY, mBillingAddressJsonObject);
        } catch (JSONException ignored) {}
        return this;
    }

    /**
     * @param postalCode Postal code of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder postalCode(String postalCode) {
        try {
            mBillingAddressJsonObject.put(POSTAL_CODE_KEY, postalCode);
            mPaymentMethodJson.put(BILLING_ADDRESS_KEY, mBillingAddressJsonObject);
        } catch (JSONException ignored) {}
        return this;
    }

    /**
     * @param region Region of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder region(String region) {
        try {
            mBillingAddressJsonObject.put(REGION_KEY, region);
            mPaymentMethodJson.put(BILLING_ADDRESS_KEY, mBillingAddressJsonObject);
        } catch (JSONException ignored) {}
        return this;
    }

    /**
     * @param streetAddress Street address of the card.
     * @return {@link com.braintreepayments.api.models.CardBuilder}
     */
    public CardBuilder streetAddress(String streetAddress) {
        try {
            mBillingAddressJsonObject.put(STREET_ADDRESS_KEY, streetAddress);
            mPaymentMethodJson.put(BILLING_ADDRESS_KEY, mBillingAddressJsonObject);
        } catch (JSONException ignored) {}
        return this;
    }

    @Override
    public String getApiPath() {
        return "credit_cards";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return Card.PAYMENT_METHOD_TYPE;
    }
}