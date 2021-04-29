package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class used to build various types of cards
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

    protected String cardholderName;
    protected String number;
    protected String company;
    protected String countryCode;
    protected String cvv;
    protected String expirationMonth;
    protected String expirationYear;
    protected String extendedAddress;
    protected String firstName;
    protected String lastName;
    protected String locality;
    protected String postalCode;
    protected String region;
    protected String streetAddress;

    public BaseCard() {
    }

    /**
     * @param number The card number.
     */
    public void setNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            this.number = null;
        } else {
            this.number = number;
        }
    }

    /**
     * @param cvv The card verification code (like CVV or CID). If you wish to create a CVV-only payment method nonce to verify a card already stored in your Vault, omit all other properties to only collect CVV.
     */
    public void setCvv(String cvv) {
        if (TextUtils.isEmpty(cvv)) {
            this.cvv = null;
        } else {
            this.cvv = cvv;
        }
    }

    /**
     * @param expirationMonth The expiration month of the card.
     */
    public void setExpirationMonth(String expirationMonth) {
        if (TextUtils.isEmpty(expirationMonth)) {
            this.expirationMonth = null;
        } else {
            this.expirationMonth = expirationMonth;
        }
    }

    /**
     * @param expirationYear The expiration year of the card.
     */
    public void setExpirationYear(String expirationYear) {
        if (TextUtils.isEmpty(expirationYear)) {
            this.expirationYear = null;
        } else {
            this.expirationYear = expirationYear;
        }
    }

    /**
     * @param expirationDate The expiration date of the card. May be in the form MM/YY or MM/YYYY.
     */
    public void setExpirationDate(String expirationDate) {
        if (TextUtils.isEmpty(expirationDate)) {
            expirationMonth = null;
            expirationYear = null;
        } else {
            String[] splitExpiration = expirationDate.split("/");

            expirationMonth = splitExpiration[0];

            if (splitExpiration.length > 1) {
                expirationYear = splitExpiration[1];
            }
        }
    }

    /**
     * @param cardholderName Name on the card.
     */
    public void setCardholderName(String cardholderName) {
        if (TextUtils.isEmpty(cardholderName)) {
            this.cardholderName = null;
        } else {
            this.cardholderName = cardholderName;
        }
    }

    /**
     * @param firstName First name on the card.
     */
    public void setFirstName(String firstName) {
        if (TextUtils.isEmpty(firstName)) {
            this.firstName = null;
        } else {
            this.firstName = firstName;
        }
    }

    /**
     * @param lastName Last name on the card.
     */
    public void setLastName(String lastName) {
        if (TextUtils.isEmpty(lastName)) {
            this.lastName = null;
        } else {
            this.lastName = lastName;
        }
    }

    /**
     * @param company Company associated with the card.
     */
    public void setCompany(String company) {
        if (TextUtils.isEmpty(company)) {
            this.company = null;
        } else {
            this.company = company;
        }
    }

    /**
     * @param countryCode The ISO 3166-1 alpha-3 country code specified in the card's billing address.
     */
    public void setCountryCode(String countryCode) {
        if (TextUtils.isEmpty(countryCode)) {
            this.countryCode = null;
        } else {
            this.countryCode = countryCode;
        }
    }

    /**
     * @param locality Locality of the card.
     */
    public void setLocality(String locality) {
        if (TextUtils.isEmpty(locality)) {
            this.locality = null;
        } else {
            this.locality = locality;
        }
    }

    /**
     * @param postalCode Postal code of the card.
     */
    public void setPostalCode(String postalCode) {
        if (TextUtils.isEmpty(postalCode)) {
            this.postalCode = null;
        } else {
            this.postalCode = postalCode;
        }
    }

    /**
     * @param region Region of the card.
     */
    public void setRegion(String region) {
        if (TextUtils.isEmpty(region)) {
            this.region = null;
        } else {
            this.region = region;
        }
    }

    /**
     * @param streetAddress Street address of the card.
     */
    public void setStreetAddress(String streetAddress) {
        if (TextUtils.isEmpty(streetAddress)) {
            this.streetAddress = null;
        } else {
            this.streetAddress = streetAddress;
        }
    }

    /**
     * @param extendedAddress address of the card.
     */
    public void setExtendedAddress(String extendedAddress) {
        if (TextUtils.isEmpty(extendedAddress)) {
            this.extendedAddress = null;
        } else {
            this.extendedAddress = extendedAddress;
        }
    }

    @Override
    JSONObject buildJSON() throws JSONException {
        JSONObject json = super.buildJSON();

        JSONObject paymentMethodNonceJson = new JSONObject();
        paymentMethodNonceJson.put(NUMBER_KEY, number);
        paymentMethodNonceJson.put(CVV_KEY, cvv);
        paymentMethodNonceJson.put(EXPIRATION_MONTH_KEY, expirationMonth);
        paymentMethodNonceJson.put(EXPIRATION_YEAR_KEY, expirationYear);

        paymentMethodNonceJson.put(CARDHOLDER_NAME_KEY, cardholderName);

        JSONObject billingAddressJson = new JSONObject();
        billingAddressJson.put(FIRST_NAME_KEY, firstName);
        billingAddressJson.put(LAST_NAME_KEY, lastName);
        billingAddressJson.put(COMPANY_KEY, company);
        billingAddressJson.put(LOCALITY_KEY, locality);
        billingAddressJson.put(POSTAL_CODE_KEY, postalCode);
        billingAddressJson.put(REGION_KEY, region);
        billingAddressJson.put(STREET_ADDRESS_KEY, streetAddress);
        billingAddressJson.put(EXTENDED_ADDRESS_KEY, extendedAddress);

        if (countryCode != null) {
            billingAddressJson.put(COUNTRY_CODE_ALPHA3_KEY, countryCode);
        }

        if (billingAddressJson.length() > 0) {
            paymentMethodNonceJson.put(BILLING_ADDRESS_KEY, billingAddressJson);
        }
        json.put(CREDIT_CARD_KEY, paymentMethodNonceJson);
        return json;
    }

    @Override
    String getApiPath() {
        return "credit_cards";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected BaseCard(Parcel in) {
        super(in);
        number = in.readString();
        cvv = in.readString();
        expirationMonth = in.readString();
        expirationYear = in.readString();
        cardholderName = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        company = in.readString();
        countryCode = in.readString();
        locality = in.readString();
        postalCode = in.readString();
        region = in.readString();
        streetAddress = in.readString();
        extendedAddress = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(number);
        dest.writeString(cvv);
        dest.writeString(expirationMonth);
        dest.writeString(expirationYear);
        dest.writeString(cardholderName);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(company);
        dest.writeString(countryCode);
        dest.writeString(locality);
        dest.writeString(postalCode);
        dest.writeString(region);
        dest.writeString(streetAddress);
        dest.writeString(extendedAddress);
    }
}
