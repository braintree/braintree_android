package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

public class ThreeDSecureV2TextBoxCustomization extends ThreeDSecureV2BaseCustomization<ThreeDSecureV2TextBoxCustomization> implements Parcelable {

    private int borderWidth;
    private String borderColor;
    private int cornerRadius;

    public ThreeDSecureV2TextBoxCustomization() {}

    public ThreeDSecureV2TextBoxCustomization borderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        return this;
    }

    public ThreeDSecureV2TextBoxCustomization borderColor(String borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    public ThreeDSecureV2TextBoxCustomization cornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(borderWidth);
        parcel.writeString(borderColor);
        parcel.writeInt(cornerRadius);
    }

    private ThreeDSecureV2TextBoxCustomization(Parcel in) {
        borderWidth = in.readInt();
        borderColor = in.readString();
        cornerRadius = in.readInt();
    }

    public static final Creator<ThreeDSecureV2TextBoxCustomization> CREATOR = new Creator<ThreeDSecureV2TextBoxCustomization>() {
        @Override
        public ThreeDSecureV2TextBoxCustomization createFromParcel(Parcel in) {
            return new ThreeDSecureV2TextBoxCustomization(in);
        }

        @Override
        public ThreeDSecureV2TextBoxCustomization[] newArray(int size) {
            return new ThreeDSecureV2TextBoxCustomization[size];
        }
    };
}
