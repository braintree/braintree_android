package com.braintreepayments.api.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

public class VisaCheckoutUserData implements Parcelable {

    private final String mUserFirstName;
    private final String mUserLastName;
    private final String mUserFullName;
    private final String mUserName;
    private final String mUserEmail;

    public VisaCheckoutUserData(JSONObject json) {
        mUserFirstName = json.optString("userFirstName");
        mUserLastName = json.optString("userLastName");
        mUserFullName = json.optString("userFullName");
        mUserName = json.optString("userName");
        mUserEmail = json.optString("userEmail");
    }

    public VisaCheckoutUserData(Parcel in) {
        mUserFirstName = in.readString();
        mUserLastName = in.readString();
        mUserFullName = in.readString();
        mUserName = in.readString();
        mUserEmail = in.readString();
    }

    public String getUserFirstName() {
        return mUserFirstName;
    }

    public String getUserLastName() {
        return mUserLastName;
    }

    public String getUserFullName() {
        return mUserFullName;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getUserEmail() {
        return mUserEmail;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUserFirstName);
        dest.writeString(mUserLastName);
        dest.writeString(mUserFullName);
        dest.writeString(mUserName);
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

    @Override
    public String toString() {
        return "VisaCheckoutUserData{" +
                "mUserFirstName='" + mUserFirstName + '\'' +
                ", mUserLastName='" + mUserLastName + '\'' +
                ", mUserFullName='" + mUserFullName + '\'' +
                ", mUserName='" + mUserName + '\'' +
                ", mUserEmail='" + mUserEmail + '\'' +
                '}';
    }
}
