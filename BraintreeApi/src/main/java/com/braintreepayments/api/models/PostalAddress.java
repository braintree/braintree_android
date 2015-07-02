package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.Serializable;

/**
 * Used by {@link com.braintreepayments.api.models.PayPalAccount} to represent billingAddress/shippingAddress
 */
public class PostalAddress implements Parcelable, Serializable {

    @SerializedName("recipientName") private String mRecipientName;
    @SerializedName("street1") private String mStreetAddress;
    @SerializedName("street2") private String mExtendedAddress;
    @SerializedName("city") private String mLocality;
    @SerializedName("country") private String mCountryCodeAlpha2;
    @SerializedName("postalCode") private String mPostalCode;
    @SerializedName("state") private String mRegion;

    public PostalAddress() {}

    public static PostalAddress addressFromJson(JSONObject addressJson) {
        if (addressJson != null) {
            PostalAddress address = new Gson().fromJson(addressJson.toString(), PostalAddress.class);
            if (address.mStreetAddress == null) {
                address.mStreetAddress = addressJson.optString("line1");
            }
            if (address.mExtendedAddress == null) {
                address.mExtendedAddress = addressJson.optString("line2");
            }
            if (address.mCountryCodeAlpha2 == null) {
                address.mCountryCodeAlpha2 = addressJson.optString("countryCode");
            }
            return address;
        }
        return null;
    }

    public String getStreetAddress() {
        return mStreetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        mStreetAddress = streetAddress;
    }

    public String getExtendedAddress() {
        return mExtendedAddress;
    }

    public void setExtendedAddress(String extendedAddress) {
        mExtendedAddress = extendedAddress;
    }

    public String getLocality() {
        return mLocality;
    }

    public void setLocality(String locality) {
        mLocality = locality;
    }

    public String getCountryCodeAlpha2() {
        return mCountryCodeAlpha2;
    }

    public void setCountryCodeAlpha2(String countryCodeAlpha2) {
        mCountryCodeAlpha2 = countryCodeAlpha2;
    }

    public String getPostalCode() {
        return mPostalCode;
    }

    public void setPostalCode(String postalCode) {
        mPostalCode = postalCode;
    }

    public String getRegion() {
        return mRegion;
    }

    public void setRegion(String region) {
        mRegion = region;
    }

    public String getRecipientName() {
        return mRecipientName;
    }

    public void setRecipientName(String recipientName) {
        mRecipientName = recipientName;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mRecipientName);
        dest.writeString(mStreetAddress);
        dest.writeString(mExtendedAddress);
        dest.writeString(mLocality);
        dest.writeString(mCountryCodeAlpha2);
        dest.writeString(mPostalCode);
        dest.writeString(mRegion);
    }

    private PostalAddress(Parcel in) {
        mRecipientName = in.readString();
        mStreetAddress = in.readString();
        mExtendedAddress = in.readString();
        mLocality = in.readString();
        mCountryCodeAlpha2 = in.readString();
        mPostalCode = in.readString();
        mRegion = in.readString();
    }

    public static final Creator<PostalAddress> CREATOR = new Creator<PostalAddress>() {
        public PostalAddress createFromParcel(Parcel source) {
            return new PostalAddress(source);
        }

        public PostalAddress[] newArray(int size) {return new PostalAddress[size];}
    };

}
