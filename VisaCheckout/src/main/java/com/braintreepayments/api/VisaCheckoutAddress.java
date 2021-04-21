package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * A class containing Visa Checkout information about the user's address.
 */
public class VisaCheckoutAddress implements Parcelable {

    private String mFirstName;
    private String mLastName;
    private String mStreetAddress;
    private String mExtendedAddress;
    private String mLocality;
    private String mRegion;
    private String mPostalCode;
    private String mCountryCode;
    private String mPhoneNumber;

    public static VisaCheckoutAddress fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        VisaCheckoutAddress visaCheckoutAddress = new VisaCheckoutAddress();

        visaCheckoutAddress.mFirstName = Json.optString(json, "firstName", "");
        visaCheckoutAddress.mLastName = Json.optString(json, "lastName", "");
        visaCheckoutAddress.mStreetAddress = Json.optString(json, "streetAddress", "");
        visaCheckoutAddress.mExtendedAddress = Json.optString(json, "extendedAddress", "");
        visaCheckoutAddress.mLocality = Json.optString(json, "locality", "");
        visaCheckoutAddress.mRegion = Json.optString(json, "region", "");
        visaCheckoutAddress.mPostalCode = Json.optString(json, "postalCode", "");
        visaCheckoutAddress.mCountryCode = Json.optString(json, "countryCode", "");
        visaCheckoutAddress.mPhoneNumber = Json.optString(json, "phoneNumber", "");

        return visaCheckoutAddress;
    }

    public VisaCheckoutAddress() {}

    /**
     * @return The user's first name.
     */
    public String getFirstName() {
        return mFirstName;
    }

    /**
     * @return The user's last name.
     */
    public String getLastName() {
        return mLastName;
    }

    /**
     * @return The user's street address.
     */
    public String getStreetAddress() {
        return mStreetAddress;
    }

    /**
     * @return The user's extended address.
     */
    public String getExtendedAddress() {
        return mExtendedAddress;
    }

    /**
     * @return The user's locality.
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
     * @return The user's postal code.
     */
    public String getPostalCode() {
        return mPostalCode;
    }

    /**
     * @return The user's country code.
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    /**
     * @return The user's phone number.
     */
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public VisaCheckoutAddress(Parcel in) {
        mFirstName = in.readString();
        mLastName = in.readString();
        mStreetAddress = in.readString();
        mExtendedAddress = in.readString();
        mLocality = in.readString();
        mRegion = in.readString();
        mPostalCode = in.readString();
        mCountryCode = in.readString();
        mPhoneNumber = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mFirstName);
        dest.writeString(mLastName);
        dest.writeString(mStreetAddress);
        dest.writeString(mExtendedAddress);
        dest.writeString(mLocality);
        dest.writeString(mRegion);
        dest.writeString(mPostalCode);
        dest.writeString(mCountryCode);
        dest.writeString(mPhoneNumber);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VisaCheckoutAddress> CREATOR = new Creator<VisaCheckoutAddress>() {
        @Override
        public VisaCheckoutAddress createFromParcel(Parcel in) {
            return new VisaCheckoutAddress(in);
        }

        @Override
        public VisaCheckoutAddress[] newArray(int size) {
            return new VisaCheckoutAddress[size];
        }
    };
}
