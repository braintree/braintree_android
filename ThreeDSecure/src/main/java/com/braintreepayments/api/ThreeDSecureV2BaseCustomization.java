package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

public class ThreeDSecureV2BaseCustomization<T> implements Parcelable {

    private String textFontName;
    private String textColor;
    private int textFontSize;

    public ThreeDSecureV2BaseCustomization() {}

    public ThreeDSecureV2BaseCustomization textFontName(String textFontName) {
        this.textFontName = textFontName;
        return this;
    }

    public ThreeDSecureV2BaseCustomization textColor(String textColor) {
        this.textColor = textColor;
        return this;
    }

    public ThreeDSecureV2BaseCustomization textFontSize(int textFontSize) {
        this.textFontSize = textFontSize;
        return this;
    }

    public String getTextFontName() {
        return textFontName;
    }

    public String getTextColor() {
        return textColor;
    }

    public int getTextFontSize() {
        return textFontSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(textFontName);
        parcel.writeString(textColor);
        parcel.writeInt(textFontSize);
    }

    private ThreeDSecureV2BaseCustomization(Parcel in) {
        textFontName = in.readString();
        textColor = in.readString();
        textFontSize = in.readInt();
    }

    public static final Creator<ThreeDSecureV2BaseCustomization> CREATOR = new Creator<ThreeDSecureV2BaseCustomization>() {
        @Override
        public ThreeDSecureV2BaseCustomization createFromParcel(Parcel in) {
            return new ThreeDSecureV2BaseCustomization(in);
        }

        @Override
        public ThreeDSecureV2BaseCustomization[] newArray(int size) {
            return new ThreeDSecureV2BaseCustomization[size];
        }
    };
}
