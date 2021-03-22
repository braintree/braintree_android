package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base builder class used to build various types of cards
 */
public abstract class BaseCard extends PaymentMethod implements Parcelable {

    static final String BILLING_ADDRESS_KEY = "billingAddress";
    static final String CARDHOLDER_NAME_KEY = "cardholderName";
    static final String COMPANY_KEY = "company";
    static final String COUNTRY_CODE_ALPHA3_KEY = "countryCodeAlpha3";
    static final String COUNTRY_CODE_KEY = "countryCode";
    static final String CREDIT_CARD_KEY = "creditCard";
    static final String CVV_KEY = "cvv";
    static final String EXPIRATION_MONTH_KEY = "expirationMonth";
    static final String EXPIRATION_YEAR_KEY = "expirationYear";
    static final String EXTENDED_ADDRESS_KEY = "extendedAddress";
    static final String FIRST_NAME_KEY = "firstName";
    static final String LAST_NAME_KEY = "lastName";
    static final String LOCALITY_KEY = "locality";
    static final String NUMBER_KEY = "number";
    static final String POSTAL_CODE_KEY = "postalCode";
    static final String REGION_KEY = "region";
    static final String STREET_ADDRESS_KEY = "streetAddress";

    String mCardholderName;
    String mCardnumber;
    String mCompany;
    String mCountryCode;
    String mCvv;
    String mExpirationMonth;
    String mExpirationYear;
    String mExtendedAddress;
    String mFirstName;
    String mLastName;
    String mLocality;
    String mPostalCode;
    String mRegion;
    String mStreetAddress;

    public BaseCard() {}

    /**
     * @param number The card number.
     */
    public void setCardNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            mCardnumber = null;
        } else {
            mCardnumber = number;
        }
    }

    /**
     * @param cvv The card verification code (like CVV or CID). If you wish to create a CVV-only payment method nonce to verify a card already stored in your Vault, omit all other properties to only collect CVV.
     */
    public void setCvv(String cvv) {
        if (TextUtils.isEmpty(cvv)) {
            mCvv = null;
        } else {
            mCvv = cvv;
        }
    }

    /**
     * @param expirationMonth The expiration month of the card.
     */
    public void setExpirationMonth(String expirationMonth) {
        if (TextUtils.isEmpty(expirationMonth)) {
            mExpirationMonth = null;
        } else {
            mExpirationMonth = expirationMonth;
        }
    }

    /**
     * @param expirationYear The expiration year of the card.
     */
    public void setExpirationYear(String expirationYear) {
        if (TextUtils.isEmpty(expirationYear)) {
            mExpirationYear = null;
        } else {
            mExpirationYear = expirationYear;
        }
    }

    /**
     * @param expirationDate The expiration date of the card. May be in the form MM/YY or MM/YYYY.
     */
    public void setExpirationDate(String expirationDate) {
        if (TextUtils.isEmpty(expirationDate)) {
            mExpirationMonth = null;
            mExpirationYear = null;
        } else {
            String[] splitExpiration = expirationDate.split("/");

            mExpirationMonth = splitExpiration[0];

            if (splitExpiration.length > 1) {
                mExpirationYear = splitExpiration[1];
            }
        }
    }

    /**
     * @param cardholderName Name on the card.
     */
    public void setCardholderName(String cardholderName) {
        if (TextUtils.isEmpty(cardholderName)) {
            mCardholderName = null;
        } else {
            mCardholderName = cardholderName;
        }
    }

    /**
     * @param firstName First name on the card.
     */
    public void setFirstName(String firstName) {
        if (TextUtils.isEmpty(firstName)) {
            mFirstName = null;
        } else {
            mFirstName = firstName;
        }
    }

    /**
     * @param lastName Last name on the card.
     */
    public void setLastName(String lastName) {
        if (TextUtils.isEmpty(lastName)) {
            mLastName = null;
        } else {
            mLastName = lastName;
        }
    }

    /**
     * @param company Company associated with the card.
     */
    public void setCompany(String company) {
        if (TextUtils.isEmpty(company)) {
            mCompany = null;
        } else {
            mCompany = company;
        }
    }

    /**
     * @param countryCode The ISO 3166-1 alpha-3 country code specified in the card's billing address.
     */
    public void setCountryCode(String countryCode) {
        if (TextUtils.isEmpty(countryCode)) {
            mCountryCode = null;
        } else {
            mCountryCode = countryCode;
        }
    }

    /**
     * @param locality Locality of the card.
     */
    public void setLocality(String locality) {
        if (TextUtils.isEmpty(locality)) {
            mLocality = null;
        } else {
            mLocality = locality;
        }
    }

    /**
     * @param postalCode Postal code of the card.
     */
    public void setPostalCode(String postalCode) {
        if (TextUtils.isEmpty(postalCode)) {
            mPostalCode = null;
        } else {
            mPostalCode = postalCode;
        }
    }

    /**
     * @param region Region of the card.
     */
    public void setRegion(String region) {
        if (TextUtils.isEmpty(region)) {
            mRegion = null;
        } else {
            mRegion = region;
        }
    }

    /**
     * @param streetAddress Street address of the card.
     */
    public void setStreetAddress(String streetAddress) {
        if (TextUtils.isEmpty(streetAddress)) {
            mStreetAddress = null;
        } else {
            mStreetAddress = streetAddress;
        }
    }

    /**
     * @param extendedAddress  address of the card.
     */
    public void setExtendedAddress(String extendedAddress) {
        if (TextUtils.isEmpty(extendedAddress)) {
            mExtendedAddress = null;
        } else {
            mExtendedAddress = extendedAddress;
        }
    }

    @Override
    protected void buildJSON(JSONObject json, JSONObject paymentMethodNonceJson) throws JSONException {
        paymentMethodNonceJson.put(NUMBER_KEY, mCardnumber);
        paymentMethodNonceJson.put(CVV_KEY, mCvv);
        paymentMethodNonceJson.put(EXPIRATION_MONTH_KEY, mExpirationMonth);
        paymentMethodNonceJson.put(EXPIRATION_YEAR_KEY, mExpirationYear);

        paymentMethodNonceJson.put(CARDHOLDER_NAME_KEY, mCardholderName);

        JSONObject billingAddressJson = new JSONObject();
        billingAddressJson.put(FIRST_NAME_KEY, mFirstName);
        billingAddressJson.put(LAST_NAME_KEY, mLastName);
        billingAddressJson.put(COMPANY_KEY, mCompany);
        billingAddressJson.put(LOCALITY_KEY, mLocality);
        billingAddressJson.put(POSTAL_CODE_KEY, mPostalCode);
        billingAddressJson.put(REGION_KEY, mRegion);
        billingAddressJson.put(STREET_ADDRESS_KEY, mStreetAddress);
        billingAddressJson.put(EXTENDED_ADDRESS_KEY, mExtendedAddress);

        if (mCountryCode != null) {
            billingAddressJson.put(COUNTRY_CODE_ALPHA3_KEY, mCountryCode);
        }

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

    protected BaseCard(Parcel in) {
        super(in);
        mCardnumber = in.readString();
        mCvv = in.readString();
        mExpirationMonth = in.readString();
        mExpirationYear = in.readString();
        mCardholderName = in.readString();
        mFirstName = in.readString();
        mLastName = in.readString();
        mCompany = in.readString();
        mCountryCode = in.readString();
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
        dest.writeString(mCardholderName);
        dest.writeString(mFirstName);
        dest.writeString(mLastName);
        dest.writeString(mCompany);
        dest.writeString(mCountryCode);
        dest.writeString(mLocality);
        dest.writeString(mPostalCode);
        dest.writeString(mRegion);
        dest.writeString(mStreetAddress);
        dest.writeString(mExtendedAddress);
    }
}
