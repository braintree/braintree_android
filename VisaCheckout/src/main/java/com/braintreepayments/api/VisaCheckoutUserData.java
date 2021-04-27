package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * A class containing Visa Checkout information about the user.
 */
public class VisaCheckoutUserData implements Parcelable {

    private String userFirstName;
    private String userLastName;
    private String userFullName;
    private String username;
    private String userEmail;

    public static VisaCheckoutUserData fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        VisaCheckoutUserData visaCheckoutUserData = new VisaCheckoutUserData();

        visaCheckoutUserData.userFirstName = Json.optString(json, "userFirstName", "");
        visaCheckoutUserData.userLastName = Json.optString(json, "userLastName", "");
        visaCheckoutUserData.userFullName = Json.optString(json, "userFullName", "");
        visaCheckoutUserData.username = Json.optString(json, "userName", "");
        visaCheckoutUserData.userEmail = Json.optString(json, "userEmail", "");

        return visaCheckoutUserData;
    }

    public VisaCheckoutUserData() {}

    /**
     * @return The user's first name.
     */
    public String getUserFirstName() {
        return userFirstName;
    }

    /**
     * @return The user's last name.
     */
    public String getUserLastName() {
        return userLastName;
    }

    /**
     * @return The user's full name.
     */
    public String getUserFullName() {
        return userFullName;
    }

    /**
     * @return The user's username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return The user's email.
     */
    public String getUserEmail() {
        return userEmail;
    }

    public VisaCheckoutUserData(Parcel in) {
        userFirstName = in.readString();
        userLastName = in.readString();
        userFullName = in.readString();
        username = in.readString();
        userEmail = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userFirstName);
        dest.writeString(userLastName);
        dest.writeString(userFullName);
        dest.writeString(username);
        dest.writeString(userEmail);
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
