package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class containing 3DS information about a postal address
 */
public class ThreeDSecurePostalAddress implements Parcelable {

    protected static final String FIRST_NAME_KEY = "firstName";
    protected static final String LAST_NAME_KEY = "lastName";
    protected static final String STREET_ADDRESS_KEY = "line1";
    protected static final String EXTENDED_ADDRESS_KEY = "line2";
    protected static final String LINE_3_KEY = "line3";
    protected static final String LOCALITY_KEY = "city";
    protected static final String REGION_KEY = "state";
    protected static final String POSTAL_CODE_KEY = "postalCode";
    protected static final String COUNTRY_CODE_ALPHA_2_KEY = "countryCode";
    protected static final String PHONE_NUMBER_KEY = "phoneNumber";
    protected static final String BILLING_ADDRESS_KEY = "billingAddress";

    private String mGivenName;
    private String mSurname;
    private String mStreetAddress;
    private String mExtendedAddress;
    private String mLine3;
    private String mLocality;
    private String mRegion;
    private String mPostalCode;
    private String mCountryCodeAlpha2;
    private String mPhoneNumber;

    public ThreeDSecurePostalAddress() {}

    /**
     * @deprecated Use {@link #givenName(String)}.
     */
    @Deprecated
    public ThreeDSecurePostalAddress firstName(String firstName) {
        givenName(firstName);
        return this;
    }

    /**
     * Optional. Set the given name
     *
     * @param givenName Given name associated with the address.
     */
    public ThreeDSecurePostalAddress givenName(String givenName) {
        mGivenName = givenName;
        return this;
    }

    /**
     * @deprecated Use {@link #surname(String)}.
     */
    @Deprecated
    public ThreeDSecurePostalAddress lastName(String lastName) {
        surname(lastName);
        return this;
    }

    /**
     * Optional. Set the surname
     *
     * @param surname Surname associated with the address.
     */
    public ThreeDSecurePostalAddress surname(String surname) {
        mSurname = surname;
        return this;
    }

    /**
     * Optional. Set the streetAddress
     *
     * @param streetAddress Line 1 of the Address (eg. number, street, etc).
     * */
    public ThreeDSecurePostalAddress streetAddress(String streetAddress) {
        mStreetAddress = streetAddress;
        return this;
    }

    /**
     * Optional. Set the extendedAddress
     *
     * @param extendedAddress Line 2 of the Address (eg. suite, apt #, etc.).
     * */
    public ThreeDSecurePostalAddress extendedAddress(String extendedAddress) {
        mExtendedAddress = extendedAddress;
        return this;
    }

    /**
     * Optional. Set line 3 of the address
     *
     * @param line3 Line 3 of the Address (eg. suite, apt #, etc.).
     * */
    public ThreeDSecurePostalAddress line3(String line3) {
        mLine3 = line3;
        return this;
    }

    /**
     * Optional. Set the locality
     *
     * @param locality City name.
     * */
    public ThreeDSecurePostalAddress locality(String locality) {
        mLocality = locality;
        return this;
    }

    /**
     * Optional. Set the region
     *
     * @param region 2 letter code for US states, and the equivalent for other countries.
     * */
    public ThreeDSecurePostalAddress region(String region) {
        mRegion = region;
        return this;
    }

    /**
     * Optional. Set the postalCode
     * For a list of countries that do not have postal codes please refer to http://en.wikipedia.org/wiki/Postal_code
     *
     * @param postalCode Zip code or equivalent is usually required for countries that have them.
     * */
    public ThreeDSecurePostalAddress postalCode(String postalCode) {
        mPostalCode = postalCode;
        return this;
    }

    /**
     * Optional. Set the countryCodeAlpha2
     *
     * @param countryCodeAlpha2 2 letter country code.
     * */
    public ThreeDSecurePostalAddress countryCodeAlpha2(String countryCodeAlpha2) {
        mCountryCodeAlpha2 = countryCodeAlpha2;
        return this;
    }

    /**
     * Optional. Set the phoneNumber
     *
     * @param phoneNumber The phone number associated with the address. Only numbers. Remove dashes, parentheses and other characters.
     * */
    public ThreeDSecurePostalAddress phoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
        return this;
    }

    /**
     * @deprecated Use {@link #getGivenName()}.
     */
    @Deprecated
    public String getFirstName() {
        return mGivenName;
    }

    /**
     * @return Given name associated with the address.
     */
    public String getGivenName() {
        return mGivenName;
    }

    /**
     * @deprecated Use {@link #getSurname()}.
     */
    @Deprecated
    public String getLastName() {
        return mSurname;
    }

    /**
     * @return Surname associated with the address.
     */
    public String getSurname() {
        return mSurname;
    }

    /**
     * @return Line 1 of the Address (eg. number, street, etc).
     */
    public String getStreetAddress() {
        return mStreetAddress;
    }

    /**
     * @return Line 2 of the Address (eg. suite, apt #, etc.).
     */
    public String getExtendedAddress() {
        return mExtendedAddress;
    }

    /**
     * @return Line 3 of the Address (eg. suite, apt #, etc.).
     */
    public String getLine3() {
        return mLine3;
    }

    /**
     * @return City name.
     */
    public String getLocality() {
        return mLocality;
    }

    /**
     * @return The user's region.
     */
    public String getRegion() {
        return mRegion;
    }

    /**
     * @return Zip code or equivalent.
     */
    public String getPostalCode() {
        return mPostalCode;
    }

    /**
     * @return 2 letter country code.
     */
    public String getCountryCodeAlpha2() {
        return mCountryCodeAlpha2;
    }

    /**
     * @return The phone number associated with the address.
     */
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public ThreeDSecurePostalAddress(Parcel in) {
        mGivenName = in.readString();
        mSurname = in.readString();
        mStreetAddress = in.readString();
        mExtendedAddress = in.readString();
        mLine3 = in.readString();
        mLocality = in.readString();
        mRegion = in.readString();
        mPostalCode = in.readString();
        mCountryCodeAlpha2 = in.readString();
        mPhoneNumber = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mGivenName);
        dest.writeString(mSurname);
        dest.writeString(mStreetAddress);
        dest.writeString(mExtendedAddress);
        dest.writeString(mLine3);
        dest.writeString(mLocality);
        dest.writeString(mRegion);
        dest.writeString(mPostalCode);
        dest.writeString(mCountryCodeAlpha2);
        dest.writeString(mPhoneNumber);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ThreeDSecurePostalAddress> CREATOR = new Creator<ThreeDSecurePostalAddress>() {
        @Override
        public ThreeDSecurePostalAddress createFromParcel(Parcel in) {
            return new ThreeDSecurePostalAddress(in);
        }

        @Override
        public ThreeDSecurePostalAddress[] newArray(int size) {
            return new ThreeDSecurePostalAddress[size];
        }
    };

    /**
     * @return JSONObject representation of {@link ThreeDSecurePostalAddress}.
     */
    public JSONObject toJson() {
        JSONObject base = new JSONObject();
        JSONObject billingAddress = new JSONObject();

        try {
            base.putOpt(ThreeDSecurePostalAddress.FIRST_NAME_KEY, mGivenName);
            base.putOpt(ThreeDSecurePostalAddress.LAST_NAME_KEY, mSurname);
            base.putOpt(ThreeDSecurePostalAddress.PHONE_NUMBER_KEY, mPhoneNumber);

            billingAddress.putOpt(ThreeDSecurePostalAddress.STREET_ADDRESS_KEY, mStreetAddress);
            billingAddress.putOpt(ThreeDSecurePostalAddress.EXTENDED_ADDRESS_KEY, mExtendedAddress);
            billingAddress.putOpt(ThreeDSecurePostalAddress.LINE_3_KEY, mLine3);
            billingAddress.putOpt(ThreeDSecurePostalAddress.LOCALITY_KEY, mLocality);
            billingAddress.putOpt(ThreeDSecurePostalAddress.REGION_KEY, mRegion);
            billingAddress.putOpt(ThreeDSecurePostalAddress.POSTAL_CODE_KEY, mPostalCode);
            billingAddress.putOpt(ThreeDSecurePostalAddress.COUNTRY_CODE_ALPHA_2_KEY, mCountryCodeAlpha2);

            if (billingAddress.length() != 0) {
                base.putOpt(ThreeDSecurePostalAddress.BILLING_ADDRESS_KEY, billingAddress);
            }
        } catch (JSONException ignored) {}

        return base;
    }
}
