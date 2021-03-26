package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * A class containing Visa Checkout information about the user.
 */
public class VisaCheckoutUserData implements Parcelable {

    private String mUserFirstName;
    private String mUserLastName;
    private String mUserFullName;
    private String mUsername;
    private String mUserEmail;

    public static VisaCheckoutUserData fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        VisaCheckoutUserData visaCheckoutUserData = new VisaCheckoutUserData();

        visaCheckoutUserData.mUserFirstName = Json.optString(json, "userFirstName", "");
        visaCheckoutUserData.mUserLastName = Json.optString(json, "userLastName", "");
        visaCheckoutUserData.mUserFullName = Json.optString(json, "userFullName", "");
        visaCheckoutUserData.mUsername = Json.optString(json, "userName", "");
        visaCheckoutUserData.mUserEmail = Json.optString(json, "userEmail", "");

        return visaCheckoutUserData;
    }

    public VisaCheckoutUserData() {}

    /**
     * @return The user's first name.
     */
    public String getUserFirstName() {
        return mUserFirstName;
    }

    /**
     * @return The user's last name.
     */
    public String getUserLastName() {
        return mUserLastName;
    }

    /**
     * @return The user's full name.
     */
    public String getUserFullName() {
        return mUserFullName;
    }

    /**
     * @return The user's username.
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * @return The user's email.
     */
    public String getUserEmail() {
        return mUserEmail;
    }

    public VisaCheckoutUserData(Parcel in) {
        mUserFirstName = in.readString();
        mUserLastName = in.readString();
        mUserFullName = in.readString();
        mUsername = in.readString();
        mUserEmail = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUserFirstName);
        dest.writeString(mUserLastName);
        dest.writeString(mUserFullName);
        dest.writeString(mUsername);
        dest.writeString(mUserEmail);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VisaCheckoutUserData> CREATOR = new Creator<VisaCheckoutUserData>() {
        @Override
        public VisaCheckoutUserData createFromParcel(Parcel in) {
            return new VisaCheckoutUserData(in);
        }

        @Override
        public VisaCheckoutUserData[] newArray(int size) {
            return new VisaCheckoutUserData[size];
        }
    };
}
