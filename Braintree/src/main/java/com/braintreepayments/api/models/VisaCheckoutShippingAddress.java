package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

public class VisaCheckoutShippingAddress implements Parcelable {
    private final String mFirstName;
    private final String mLastName;
    private final String mStreetAddress;
    private final String mLocality;
    private final String mRegion;
    private final String mPostalCode;
    private final String mCountryCode;

    public VisaCheckoutShippingAddress(JSONObject json) {
        mFirstName = json.optString("firstName");
        mLastName = json.optString("lastName");
        mStreetAddress = json.optString("streetAddress");
        mLocality = json.optString("locality");
        mRegion = json.optString("region");
        mPostalCode = json.optString("postalCode");
        mCountryCode = json.optString("countryCode");
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getStreetAddress() {
        return mStreetAddress;
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

    public String getCountryCode() {
        return mCountryCode;
    }

    public VisaCheckoutShippingAddress(Parcel in) {
        mFirstName = in.readString();
        mLastName = in.readString();
        mStreetAddress = in.readString();
        mLocality = in.readString();
        mRegion = in.readString();
        mPostalCode = in.readString();
        mCountryCode = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFirstName);
        dest.writeString(mLastName);
        dest.writeString(mStreetAddress);
        dest.writeString(mLocality);
        dest.writeString(mRegion);
        dest.writeString(mPostalCode);
        dest.writeString(mCountryCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VisaCheckoutShippingAddress> CREATOR = new Creator<VisaCheckoutShippingAddress>() {
        @Override
        public VisaCheckoutShippingAddress createFromParcel(Parcel in) {
            return new VisaCheckoutShippingAddress(in);
        }

        @Override
        public VisaCheckoutShippingAddress[] newArray(int size) {
            return new VisaCheckoutShippingAddress[size];
        }
    };

    @Override
    public String toString() {
        return "VisaCheckoutShippingAddress{" +
                "mFirstName='" + mFirstName + '\'' +
                ", mLastName='" + mLastName + '\'' +
                ", mStreetAddress='" + mStreetAddress + '\'' +
                ", mLocality='" + mLocality + '\'' +
                ", mRegion='" + mRegion + '\'' +
                ", mPostalCode='" + mPostalCode + '\'' +
                ", mCountryCode='" + mCountryCode + '\'' +
                '}';
    }
}
