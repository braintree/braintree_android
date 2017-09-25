package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

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
    protected static final String COMPANY_KEY = "company";
    protected static final String COUNTRY_NAME_KEY = "countryName";
    protected static final String COUNTRY_CODE_ALPHA2_KEY = "countryCodeAlpha2";
    protected static final String COUNTRY_CODE_ALPHA3_KEY = "countryCodeAlpha3";
    protected static final String COUNTRY_CODE_NUMERIC_KEY = "countryCodeNumeric";
    protected static final String LOCALITY_KEY = "locality";
    protected static final String POSTAL_CODE_KEY = "postalCode";
    protected static final String REGION_KEY = "region";
    protected static final String STREET_ADDRESS_KEY = "streetAddress";
    protected static final String EXTENDED_ADDRESS_KEY = "extendedAddress";

    protected String mCardnumber;
    protected String mCvv;
    protected String mExpirationMonth;
    protected String mExpirationYear;
    protected String mExpirationDate;
    protected String mCardholderName;
    protected String mBillingAddress;
    protected String mFirstName;
    protected String mLastName;
    protected String mCompany;
    protected String mCountryName;
    protected String mCountryCodeAlpha2;
    protected String mCountryCodeAlpha3;
    protected String mCountryCodeNumeric;
    protected String mLocality;
    protected String mPostalCode;
    protected String mRegion;
    protected String mStreetAddress;
    protected String mExtendedAddress;

    public BaseCardBuilder() {}

    /**
     * @param number The card number.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T cardNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            mCardnumber = null;
        } else {
            mCardnumber = number;
        }
        return (T) this;
    }

    /**
     * @param cvv The card's CVV.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T cvv(String cvv) {
        if (TextUtils.isEmpty(cvv)) {
            mCvv = null;
        } else {
            mCvv = cvv;
        }
        return (T) this;
    }

    /**
     * @param expirationMonth The expiration month of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T expirationMonth(String expirationMonth) {
        if (TextUtils.isEmpty(expirationMonth)) {
            mExpirationMonth = null;
        } else {
            mExpirationMonth = expirationMonth;
        }
        return (T) this;
    }

    /**
     * @param expirationYear The expiration year of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T expirationYear(String expirationYear) {
        if (TextUtils.isEmpty(expirationYear)) {
            mExpirationYear = null;
        } else {
            mExpirationYear = expirationYear;
        }
        return (T) this;
    }

    /**
     * @param expirationDate The expiration date of the card. May be in the form MM/YY or MM/YYYY.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T expirationDate(String expirationDate) {
        if (TextUtils.isEmpty(expirationDate)) {
            mExpirationDate = null;
        } else {
            mExpirationDate = expirationDate;
        }
        return (T) this;
    }

    /**
     * @param cardholderName Name on the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T cardholderName(String cardholderName) {
        if (TextUtils.isEmpty(cardholderName)) {
            mCardholderName = null;
        } else {
            mCardholderName = cardholderName;
        }
        return (T) this;
    }

    /**
     * @param firstName First name on the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T firstName(String firstName) {
        if (TextUtils.isEmpty(firstName)) {
            mFirstName = null;
        } else {
            mFirstName = firstName;
        }
        return (T) this;
    }

    /**
     * @param lastName Last name on the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T lastName(String lastName) {
        if (TextUtils.isEmpty(lastName)) {
            mLastName = null;
        } else {
            mLastName = lastName;
        }
        return (T) this;
    }

    /**
     * @param company Company associated with the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T company(String company) {
        if (TextUtils.isEmpty(company)) {
            mCompany = null;
        } else {
            mCompany = company;
        }
        return (T) this;
    }

    /**
     * @param countryName Country name of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T countryName(String countryName) {
        if (TextUtils.isEmpty(countryName)) {
            mCountryName = null;
        } else {
            mCountryName = countryName;
        }
        return (T) this;
    }

    /**
     * @param countryCodeAlpha2 The ISO 3166-1 alpha-2 country code specified in the card's billing address.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T countryCodeAlpha2(String countryCodeAlpha2) {
        if (TextUtils.isEmpty(countryCodeAlpha2)) {
            mCountryCodeAlpha2 = null;
        } else {
            mCountryCodeAlpha2 = countryCodeAlpha2;
        }
        return (T) this;
    }

    /**
     * @param countryCodeAlpha3 The ISO 3166-1 alpha-3 country code specified in the card's billing address.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T countryCodeAlpha3(String countryCodeAlpha3) {
        if (TextUtils.isEmpty(countryCodeAlpha3)) {
            mCountryCodeAlpha3 = null;
        } else {
            mCountryCodeAlpha3 = countryCodeAlpha3;
        }
        return (T) this;
    }

    /**
     * @param countryCodeNumeric The ISO 3166-1 alpha-numeric country code specified in the card's billing address.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T countryCodeNumeric(String countryCodeNumeric) {
        if (TextUtils.isEmpty(countryCodeNumeric)) {
            mCountryCodeNumeric = null;
        } else {
            mCountryCodeNumeric = countryCodeNumeric;
        }
        return (T) this;
    }

    /**
     * @param locality Locality of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T locality(String locality) {
        if (TextUtils.isEmpty(locality)) {
            mLocality = null;
        } else {
            mLocality = locality;
        }
        return (T) this;
    }

    /**
     * @param postalCode Postal code of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T postalCode(String postalCode) {
        if (TextUtils.isEmpty(postalCode)) {
            mPostalCode = null;
        } else {
            mPostalCode = postalCode;
        }
        return (T) this;
    }

    /**
     * @param region Region of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T region(String region) {
        if (TextUtils.isEmpty(region)) {
            mRegion = null;
        } else {
            mRegion = region;
        }
        return (T) this;
    }

    /**
     * @param streetAddress Street address of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T streetAddress(String streetAddress) {
        if (TextUtils.isEmpty(streetAddress)) {
            mStreetAddress = null;
        } else {
            mStreetAddress = streetAddress;
        }
        return (T) this;
    }

    /**
     * @param extendedAddress  address of the card.
     * @return {@link com.braintreepayments.api.models.BaseCardBuilder}
     */
    @SuppressWarnings("unchecked")
    public T extendedAddress(String extendedAddress) {
        if (TextUtils.isEmpty(extendedAddress)) {
            mExtendedAddress = null;
        } else {
            mExtendedAddress = extendedAddress;
        }
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
        billingAddressJson.put(COMPANY_KEY, mCompany);
        billingAddressJson.put(COUNTRY_NAME_KEY, mCountryName);
        billingAddressJson.put(COUNTRY_CODE_ALPHA2_KEY, mCountryCodeAlpha2);
        billingAddressJson.put(COUNTRY_CODE_ALPHA3_KEY, mCountryCodeAlpha3);
        billingAddressJson.put(COUNTRY_CODE_NUMERIC_KEY, mCountryCodeNumeric);
        billingAddressJson.put(LOCALITY_KEY, mLocality);
        billingAddressJson.put(POSTAL_CODE_KEY, mPostalCode);
        billingAddressJson.put(REGION_KEY, mRegion);
        billingAddressJson.put(STREET_ADDRESS_KEY, mStreetAddress);
        billingAddressJson.put(EXTENDED_ADDRESS_KEY, mExtendedAddress);

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
        mCompany = in.readString();
        mCountryName = in.readString();
        mCountryCodeAlpha2 = in.readString();
        mCountryCodeAlpha3 = in.readString();
        mCountryCodeNumeric = in.readString();
        mLocality = in.readString();
        mPostalCode = in.readString();
        mRegion = in.readString();
        mStreetAddress = in.readString();
        mExtendedAddress = in.readString();
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
        dest.writeString(mCompany);
        dest.writeString(mCountryName);
        dest.writeString(mCountryCodeAlpha2);
        dest.writeString(mCountryCodeAlpha3);
        dest.writeString(mCountryCodeNumeric);
        dest.writeString(mLocality);
        dest.writeString(mPostalCode);
        dest.writeString(mRegion);
        dest.writeString(mStreetAddress);
        dest.writeString(mExtendedAddress);
    }
}
