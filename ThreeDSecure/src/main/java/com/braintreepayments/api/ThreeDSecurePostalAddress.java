package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

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

    private String givenName;
    private String surname;
    private String streetAddress;
    private String extendedAddress;
    private String line3;
    private String locality;
    private String region;
    private String postalCode;
    private String countryCodeAlpha2;
    private String phoneNumber;

    public ThreeDSecurePostalAddress() {}

    /**
     * Optional. Set the given name
     *
     * @param givenName Given name associated with the address.
     */
    public void setGivenName(@Nullable String givenName) {
        this.givenName = givenName;
    }

    /**
     * Optional. Set the surname
     *
     * @param surname Surname associated with the address.
     */
    public void setSurname(@Nullable String surname) {
        this.surname = surname;
    }

    /**
     * Optional. Set the streetAddress
     *
     * @param streetAddress Line 1 of the Address (eg. number, street, etc).
     * */
    public void setStreetAddress(@Nullable String streetAddress) {
        this.streetAddress = streetAddress;
    }

    /**
     * Optional. Set the extendedAddress
     *
     * @param extendedAddress Line 2 of the Address (eg. suite, apt #, etc.).
     * */
    public void setExtendedAddress(@Nullable String extendedAddress) {
        this.extendedAddress = extendedAddress;
    }

    /**
     * Optional. Set line 3 of the address
     *
     * @param line3 Line 3 of the Address (eg. suite, apt #, etc.).
     * */
    public void setLine3(@Nullable String line3) {
        this.line3 = line3;
    }

    /**
     * Optional. Set the locality
     *
     * @param locality City name.
     * */
    public void setLocality(@Nullable String locality) {
        this.locality = locality;
    }

    /**
     * Optional. Set the region
     *
     * @param region Either a two-letter state code (for the US), or an ISO-3166-2 country subdivision code of up to three letters.
     * */
    public void setRegion(@Nullable String region) {
        this.region = region;
    }

    /**
     * Optional. Set the postalCode
     * For a list of countries that do not have postal codes please refer to http://en.wikipedia.org/wiki/Postal_code
     *
     * @param postalCode Zip code or equivalent is usually required for countries that have them.
     * */
    public void setPostalCode(@Nullable String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     * Optional. Set the countryCodeAlpha2
     *
     * @param countryCodeAlpha2 2 letter country code.
     * */
    public void setCountryCodeAlpha2(@Nullable String countryCodeAlpha2) {
        this.countryCodeAlpha2 = countryCodeAlpha2;
    }

    /**
     * Optional. Set the phoneNumber
     *
     * @param phoneNumber The phone number associated with the address. Only numbers. Remove dashes, parentheses and other characters.
     * */
    public void setPhoneNumber(@Nullable String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * @return Given name associated with the address.
     */
    @Nullable
    public String getGivenName() {
        return givenName;
    }

    /**
     * @return Surname associated with the address.
     */
    @Nullable
    public String getSurname() {
        return surname;
    }

    /**
     * @return Line 1 of the Address (eg. number, street, etc).
     */
    @Nullable
    public String getStreetAddress() {
        return streetAddress;
    }

    /**
     * @return Line 2 of the Address (eg. suite, apt #, etc.).
     */
    @Nullable
    public String getExtendedAddress() {
        return extendedAddress;
    }

    /**
     * @return Line 3 of the Address (eg. suite, apt #, etc.).
     */
    @Nullable
    public String getLine3() {
        return line3;
    }

    /**
     * @return City name.
     */
    @Nullable
    public String getLocality() {
        return locality;
    }

    /**
     * @return The user's region.
     */
    @Nullable
    public String getRegion() {
        return region;
    }

    /**
     * @return Zip code or equivalent.
     */
    @Nullable
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * @return 2 letter country code.
     */
    @Nullable
    public String getCountryCodeAlpha2() {
        return countryCodeAlpha2;
    }

    /**
     * @return The phone number associated with the address.
     */
    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public ThreeDSecurePostalAddress(Parcel in) {
        givenName = in.readString();
        surname = in.readString();
        streetAddress = in.readString();
        extendedAddress = in.readString();
        line3 = in.readString();
        locality = in.readString();
        region = in.readString();
        postalCode = in.readString();
        countryCodeAlpha2 = in.readString();
        phoneNumber = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(givenName);
        dest.writeString(surname);
        dest.writeString(streetAddress);
        dest.writeString(extendedAddress);
        dest.writeString(line3);
        dest.writeString(locality);
        dest.writeString(region);
        dest.writeString(postalCode);
        dest.writeString(countryCodeAlpha2);
        dest.writeString(phoneNumber);
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
            base.putOpt(ThreeDSecurePostalAddress.FIRST_NAME_KEY, givenName);
            base.putOpt(ThreeDSecurePostalAddress.LAST_NAME_KEY, surname);
            base.putOpt(ThreeDSecurePostalAddress.PHONE_NUMBER_KEY, phoneNumber);

            billingAddress.putOpt(ThreeDSecurePostalAddress.STREET_ADDRESS_KEY, streetAddress);
            billingAddress.putOpt(ThreeDSecurePostalAddress.EXTENDED_ADDRESS_KEY, extendedAddress);
            billingAddress.putOpt(ThreeDSecurePostalAddress.LINE_3_KEY, line3);
            billingAddress.putOpt(ThreeDSecurePostalAddress.LOCALITY_KEY, locality);
            billingAddress.putOpt(ThreeDSecurePostalAddress.REGION_KEY, region);
            billingAddress.putOpt(ThreeDSecurePostalAddress.POSTAL_CODE_KEY, postalCode);
            billingAddress.putOpt(ThreeDSecurePostalAddress.COUNTRY_CODE_ALPHA_2_KEY, countryCodeAlpha2);

            if (billingAddress.length() != 0) {
                base.putOpt(ThreeDSecurePostalAddress.BILLING_ADDRESS_KEY, billingAddress);
            }
        } catch (JSONException ignored) {}

        return base;
    }
}
