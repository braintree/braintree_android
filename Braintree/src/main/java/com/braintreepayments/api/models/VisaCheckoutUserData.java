package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

public class VisaCheckoutUserData implements Parcelable {


    private final String userFirstName;
    private final String userLastName;
    private final String userFullName;
    private final String userName;
    private final String userEmail;

    public VisaCheckoutUserData(JSONObject json) {
        userFirstName = json.optString("userFirstName");
        userLastName = json.optString("userLastName");
        userFullName = json.optString("userFullName");
        userName = json.optString("userName");
        userEmail = json.optString("userEmail");
    }

    public VisaCheckoutUserData(Parcel in) {
        userFirstName = in.readString();
        userLastName = in.readString();
        userFullName = in.readString();
        userName = in.readString();
        userEmail = in.readString();
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userFirstName);
        dest.writeString(userLastName);
        dest.writeString(userFullName);
        dest.writeString(userName);
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
