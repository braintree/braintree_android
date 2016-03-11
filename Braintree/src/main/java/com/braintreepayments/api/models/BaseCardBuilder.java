package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base builder class used to build various types of cards
 */
public abstract class BaseCardBuilder<T> extends PaymentMethodBuilder<T> implements Parcelable {

    protected static final String CREDIT_CARD_KEY = "creditCard";
    protected static final String NUMBER_KEY = "number";
    protected static final String EXPIRATION_MONTH_KEY = "expirationMonth";
    protected static final String EXPIRATION_YEAR_KEY = "expirationYear";
    protected static final String CVV_KEY = "cvv";
    protected static final String EXPIRATION_DATE_KEY = "expirationDate";
    protected static final String CARDHOLDER_NAME_KEY = "cardholderName";
    protected static final String BILLING_ADDRESS_KEY = "billingAddress";
    protected static final String FIRST_NAME_KEY = "firstName";
    protected static final String LAST_NAME_KEY = "lastName";
    protected static final String COUNTRY_NAME_KEY = "countryName";
    protected static final String LOCALITY_KEY = "locality";
    protected static final String POSTAL_CODE_KEY = "postalCode";
    protected static final String REGION_KEY = "region";
    protected static final String STREET_ADDRESS_KEY = "streetAddress";

    protected String mCardnumber;
    protected String mCvv;
    protected String mExpirationMonth;
    protected String mExpirationYear;
    protected String mExpirationDate;
    protected String mCardholderName;
    protected String mBillingAddress;
    protected String mFirstName;
    protected String mLastName;
    protected String mCountryName;
    protected String mLocality;
    protected String mPostalCode;
    protected String mRegion;
    protected String mStreetAddress;

    public BaseCardBuilder() {}

    /**
     * @param number The card number.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T cardNumber(String number) {
        mCardnumber = number;
        return (T) this;
    }

    /**
     * @param cvv The card's CVV.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T cvv(String cvv) {
        mCvv = cvv;
        return (T) this;
    }

    /**
     * @param expirationMonth The expiration month of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T expirationMonth(String expirationMonth) {
        mExpirationMonth = expirationMonth;
        return (T) this;
    }

    /**
     * @param expirationYear The expiration year of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T expirationYear(String expirationYear) {
        mExpirationYear = expirationYear;
        return (T) this;
    }

    /**
     * @param expirationDate The expiration date of the card. May be in the form MM/YY or MM/YYYY.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T expirationDate(String expirationDate) {
        mExpirationDate = expirationDate;
        return (T) this;
    }

    /**
     * @param cardholderName Name on the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T cardholderName(String cardholderName) {
        mCardholderName = cardholderName;
        return (T) this;
    }

    /**
     * @param firstName First name on the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T firstName(String firstName) {
        mFirstName = firstName;
        return (T) this;
    }

    /**
     * @param lastName Last name on the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T lastName(String lastName) {
        mLastName = lastName;
        return (T) this;
    }

    /**
     * @param countryName Country name of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T countryName(String countryName) {
        mCountryName = countryName;
        return (T) this;
    }

    /**
     * @param locality Locality of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T locality(String locality) {
        mLocality = locality;
        return (T) this;
    }

    /**
     * @param postalCode Postal code of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T postalCode(String postalCode) {
        mPostalCode = postalCode;
        return (T) this;
    }

    /**
     * @param region Region of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T region(String region) {
        mRegion = region;
        return (T) this;
    }

    /**
     * @param streetAddress Street address of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T streetAddress(String streetAddress) {
        mStreetAddress = streetAddress;
        return (T) this;
    }

    @Override
    protected void build(JSONObject json, JSONObject paymentMethodNonceJson) throws JSONException {
        paymentMethodNonceJson.put(NUMBER_KEY, mCardnumber);
        paymentMethodNonceJson.put(CVV_KEY, mCvv);
        paymentMethodNonceJson.put(EXPIRATION_MONTH_KEY, mExpirationMonth);
        paymentMethodNonceJson.put(EXPIRATION_YEAR_KEY, mExpirationYear);
        paymentMethodNonceJson.put(EXPIRATION_DATE_KEY, mExpirationDate);

        paymentMethodNonceJson.put(CARDHOLDER_NAME_KEY, mCardholderName);

        JSONObject billingAddressJson = new JSONObject();
        billingAddressJson.put(FIRST_NAME_KEY, mFirstName);
        billingAddressJson.put(LAST_NAME_KEY, mLastName);
        billingAddressJson.put(COUNTRY_NAME_KEY, mCountryName);
        billingAddressJson.put(LOCALITY_KEY, mLocality);
        billingAddressJson.put(POSTAL_CODE_KEY, mPostalCode);
        billingAddressJson.put(REGION_KEY, mRegion);
        billingAddressJson.put(STREET_ADDRESS_KEY, mStreetAddress);

        if (billingAddressJson.length() > 0) {
            paymentMethodNonceJson.put(BILLING_ADDRESS_KEY, billingAddressJson);
        }

        json.put(CREDIT_CARD_KEY, paymentMethodNonceJson);
    }

    @Override
    public String getApiPath() {
        return "credit_cards";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return CardNonce.TYPE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected BaseCardBuilder(Parcel in) {
        super(in);
        mCardnumber = in.readString();
        mCvv = in.readString();
        mExpirationMonth = in.readString();
        mExpirationYear = in.readString();
        mExpirationDate = in.readString();
        mCardholderName = in.readString();
        mBillingAddress = in.readString();
        mFirstName = in.readString();
        mLastName = in.readString();
        mCountryName = in.readString();
        mLocality = in.readString();
        mPostalCode = in.readString();
        mRegion = in.readString();
        mStreetAddress = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mCardnumber);
        dest.writeString(mCvv);
        dest.writeString(mExpirationMonth);
        dest.writeString(mExpirationYear);
        dest.writeString(mExpirationDate);
        dest.writeString(mCardholderName);
        dest.writeString(mBillingAddress);
        dest.writeString(mFirstName);
        dest.writeString(mLastName);
        dest.writeString(mCountryName);
        dest.writeString(mLocality);
        dest.writeString(mPostalCode);
        dest.writeString(mRegion);
        dest.writeString(mStreetAddress);
    }
}