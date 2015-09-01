package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * Java object representing a postal address
 */
public class PostalAddress implements Parcelable {

    private static final String RECIPIENT_NAME_KEY = "recipientName";
    private static final String STREET_ADDRESS_KEY = "street1";
    private static final String EXTENDED_ADDRESS_KEY = "street2";
    private static final String LOCALITY_KEY = "city";
    private static final String COUNTRY_CODE_ALPHA_2_KEY = "country";
    private static final String POSTAL_CODE_KEY = "postalCode";
    private static final String REGION_KEY = "state";
    private static final String LINE_1_KEY = "line1";
    private static final String LINE_2_KEY = "line2";
    private static final String COUNTRY_CODE_KEY = "countryCode";

    private String mRecipientName;
    private String mStreetAddress;
    private String mExtendedAddress;
    private String mLocality;
    private String mRegion;
    private String mPostalCode;
    private String mCountryCodeAlpha2;

    protected PostalAddress(String streetAddress, String extendedAddress, String locality,
            String region, String postalCode, String countryCodeAlpha2, String recipientName) {
        mRecipientName = recipientName;
        mStreetAddress = streetAddress;
        mExtendedAddress = extendedAddress;
        mLocality = locality;
        mRegion = region;
        mPostalCode = postalCode;
        mCountryCodeAlpha2 = countryCodeAlpha2;
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

    public PostalAddress() {}

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

    @Override
    public String toString() {
        return String.format("%s\n%s\n%s\n%s, %s\n%s %s", mRecipientName, mStreetAddress, mExtendedAddress, mLocality, mRegion, mPostalCode, mCountryCodeAlpha2);
    }

    public static final Creator<PostalAddress> CREATOR = new Creator<PostalAddress>() {
        public PostalAddress createFromParcel(Parcel source) {
            return new PostalAddress(source);
        }

        public PostalAddress[] newArray(int size) {
            return new PostalAddress[size];
        }
    };

    public static PostalAddress fromJson(JSONObject accountAddress) {
        // If we don't have an account address, return null.
        if (accountAddress == null) {
            return null;
        }
        String recipientName = accountAddress.optString(RECIPIENT_NAME_KEY, null);
        String streetAddress = accountAddress.optString(STREET_ADDRESS_KEY, null);
        String extendedAddress = accountAddress.optString(EXTENDED_ADDRESS_KEY, null);
        String locality = accountAddress.optString(LOCALITY_KEY, null);
        String region = accountAddress.optString(REGION_KEY, null);
        String postalCode = accountAddress.optString(POSTAL_CODE_KEY, null);
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
        return new PostalAddress(streetAddress, extendedAddress,
                locality, region, postalCode, countryCodeAlpha2, recipientName);
    }
}
