package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Java object representing a postal address
 */
public class PostalAddress implements Parcelable {

    public static final String RECIPIENT_NAME_KEY = "recipientName";
    public static final String STREET_ADDRESS_KEY = "street1";
    public static final String EXTENDED_ADDRESS_KEY = "street2";
    public static final String LOCALITY_KEY = "city";
    public static final String COUNTRY_CODE_ALPHA_2_KEY = "country";
    public static final String POSTAL_CODE_KEY = "postalCode";
    public static final String REGION_KEY = "state";
    public static final String LINE_1_KEY = "line1";
    public static final String LINE_2_KEY = "line2";
    public static final String COUNTRY_CODE_KEY = "countryCode";

    public static final String COUNTRY_CODE_UNDERSCORE_KEY = "country_code";
    public static final String POSTAL_CODE_UNDERSCORE_KEY = "postal_code";
    public static final String RECIPIENT_NAME_UNDERSCORE_KEY = "recipient_name";

    private String mRecipientName;
    private String mStreetAddress;
    private String mExtendedAddress;
    private String mLocality;
    private String mRegion;
    private String mPostalCode;
    private String mCountryCodeAlpha2;

    public PostalAddress() {}

    public static PostalAddress fromJson(JSONObject accountAddress) {
        // If we don't have an account address, return an empty PostalAddress.
        if (accountAddress == null) {
            return new PostalAddress();
        }

        String streetAddress = accountAddress.optString(STREET_ADDRESS_KEY, null);
        String extendedAddress = accountAddress.optString(EXTENDED_ADDRESS_KEY, null);
        String countryCodeAlpha2 = accountAddress.optString(COUNTRY_CODE_ALPHA_2_KEY, null);

        //Check alternate keys
        if (streetAddress == null) {
            streetAddress = accountAddress.optString(LINE_1_KEY, null);
        }
        if (extendedAddress == null) {
            extendedAddress = accountAddress.optString(LINE_2_KEY, null);
        }
        if (countryCodeAlpha2 == null) {
            countryCodeAlpha2 = accountAddress.optString(COUNTRY_CODE_KEY, null);
        }

        return new PostalAddress().recipientName(accountAddress.optString(RECIPIENT_NAME_KEY, null))
                .streetAddress(streetAddress)
                .extendedAddress(extendedAddress)
                .locality(accountAddress.optString(LOCALITY_KEY, null))
                .region(accountAddress.optString(REGION_KEY, null))
                .postalCode(accountAddress.optString(POSTAL_CODE_KEY, null))
                .countryCodeAlpha2(countryCodeAlpha2);
    }

    public PostalAddress recipientName(String name) {
        mRecipientName = name;
        return this;
    }

    public PostalAddress streetAddress(String streetAddress) {
        mStreetAddress = streetAddress;
        return this;
    }

    public PostalAddress extendedAddress(String extendedAddress) {
        mExtendedAddress = extendedAddress;
        return this;
    }

    public PostalAddress locality(String locality) {
        mLocality = locality;
        return this;
    }

    public PostalAddress region(String region) {
        mRegion = region;
        return this;
    }

    public PostalAddress postalCode(String postalCode) {
        mPostalCode = postalCode;
        return this;
    }

    public PostalAddress countryCodeAlpha2(String countryCodeAlpha2) {
        mCountryCodeAlpha2 = countryCodeAlpha2;
        return this;
    }

    public String getRecipientName() {
        return mRecipientName;
    }

    public String getStreetAddress() {
        return mStreetAddress;
    }

    public String getExtendedAddress() {
        return mExtendedAddress;
    }

    public String getLocality() {
        return mLocality;
    }

    public String getRegion() {
        return mRegion;
    }

    public String getPostalCode() {
        return mPostalCode;
    }

    public String getCountryCodeAlpha2() {
        return mCountryCodeAlpha2;
    }

    public boolean isEmpty() {
        // A PostalAddress is considered empty if it does not have a country code
        return TextUtils.isEmpty(mCountryCodeAlpha2);
    }

    @Override
    public String toString() {
        return String.format("%s\n%s\n%s\n%s, %s\n%s %s", mRecipientName, mStreetAddress,
                mExtendedAddress, mLocality, mRegion, mPostalCode, mCountryCodeAlpha2);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mStreetAddress);
        dest.writeString(mExtendedAddress);
        dest.writeString(mLocality);
        dest.writeString(mRegion);
        dest.writeString(mPostalCode);
        dest.writeString(mCountryCodeAlpha2);
        dest.writeString(mRecipientName);
    }

    private PostalAddress(Parcel in) {
        mStreetAddress = in.readString();
        mExtendedAddress = in.readString();
        mLocality = in.readString();
        mRegion = in.readString();
        mPostalCode = in.readString();
        mCountryCodeAlpha2 = in.readString();
        mRecipientName = in.readString();
    }

    public static final Creator<PostalAddress> CREATOR = new Creator<PostalAddress>() {
        public PostalAddress createFromParcel(Parcel source) {
            return new PostalAddress(source);
        }

        public PostalAddress[] newArray(int size) {
            return new PostalAddress[size];
        }
    };
}
