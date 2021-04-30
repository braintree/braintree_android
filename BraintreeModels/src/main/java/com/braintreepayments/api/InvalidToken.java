package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

class InvalidToken extends Authorization implements Parcelable {

    private final String errorMessage;

    InvalidToken(String rawValue, String errorMessage) {
        super(rawValue);
        this.errorMessage = errorMessage;
    }

    @Override
    String getConfigUrl() {
        return null;
    }

    @Override
    String getBearer() {
        return null;
    }

    @Override
    boolean isValid() {
        return false;
    }

    String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected InvalidToken(Parcel in) {
        super(in);
        errorMessage = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(errorMessage);
    }

    public static final Creator<InvalidToken> CREATOR = new Creator<InvalidToken>() {
        public InvalidToken createFromParcel(Parcel source) {
            return new InvalidToken(source);
        }

        public InvalidToken[] newArray(int size) {
            return new InvalidToken[size];
        }
    };
}
