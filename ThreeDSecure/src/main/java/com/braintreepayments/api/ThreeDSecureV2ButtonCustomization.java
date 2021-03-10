package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

public class ThreeDSecureV2ButtonCustomization extends ThreeDSecureV2BaseCustomization<ThreeDSecureV2ButtonCustomization> implements Parcelable {

    private String backgroundColor;
    private int cornerRadius;

    public ThreeDSecureV2ButtonCustomization() {}

    public ThreeDSecureV2ButtonCustomization backgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public ThreeDSecureV2ButtonCustomization cornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(backgroundColor);
        dest.writeInt(cornerRadius);
    }

    private ThreeDSecureV2ButtonCustomization(Parcel in) {
        backgroundColor = in.readString();
        cornerRadius = in.readInt();
    }

    public static final Creator<ThreeDSecureV2ButtonCustomization> CREATOR = new Creator<ThreeDSecureV2ButtonCustomization>() {
        @Override
        public ThreeDSecureV2ButtonCustomization createFromParcel(Parcel in) {
            return new ThreeDSecureV2ButtonCustomization(in);
        }

        @Override
        public ThreeDSecureV2ButtonCustomization[] newArray(int size) {
            return new ThreeDSecureV2ButtonCustomization[size];
        }
    };
}
