package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

public class ThreeDSecureV2LabelCustomization implements Parcelable {

    private String headingTextColor;
    private String headingTextFontName;
    private int headingTextFontSize;

    public ThreeDSecureV2LabelCustomization() {}

    public ThreeDSecureV2LabelCustomization headingTextColor(String headingTextColor) {
        this.headingTextColor = headingTextColor;
        return this;
    }

    public ThreeDSecureV2LabelCustomization headingTextFontName(String headingTextFontName) {
        this.headingTextFontName = headingTextFontName;
        return this;
    }

    public ThreeDSecureV2LabelCustomization headingTextFontSize(int headingTextFontSize) {
        this.headingTextFontSize = headingTextFontSize;
        return this;
    }

    public String getHeadingTextColor() {
        return headingTextColor;
    }

    public String getHeadingTextFontName() {
        return headingTextFontName;
    }

    public int getHeadingTextFontSize() {
        return headingTextFontSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private ThreeDSecureV2LabelCustomization(Parcel in) {
        headingTextColor = in.readString();
        headingTextFontName = in.readString();
        headingTextFontSize = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(headingTextColor);
        parcel.writeString(headingTextFontName);
        parcel.writeInt(headingTextFontSize);
    }

    public static final Creator<ThreeDSecureV2LabelCustomization> CREATOR = new Creator<ThreeDSecureV2LabelCustomization>() {
        @Override
        public ThreeDSecureV2LabelCustomization createFromParcel(Parcel in) {
            return new ThreeDSecureV2LabelCustomization(in);
        }

        @Override
        public ThreeDSecureV2LabelCustomization[] newArray(int size) {
            return new ThreeDSecureV2LabelCustomization[size];
        }
    };
}
