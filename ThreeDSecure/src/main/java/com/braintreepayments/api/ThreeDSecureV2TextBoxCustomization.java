package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization;

public class ThreeDSecureV2TextBoxCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private int borderWidth;
    private String borderColor;
    private int cornerRadius;
    private TextBoxCustomization cardinalValue = new TextBoxCustomization();

    public ThreeDSecureV2TextBoxCustomization() {}

    public ThreeDSecureV2TextBoxCustomization borderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        cardinalValue.setBorderWidth(borderWidth);
        return this;
    }

    public ThreeDSecureV2TextBoxCustomization borderColor(String borderColor) {
        this.borderColor = borderColor;
        cardinalValue.setBorderColor(borderColor);
        return this;
    }

    public ThreeDSecureV2TextBoxCustomization cornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        cardinalValue.setCornerRadius(cornerRadius);
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

    TextBoxCustomization getCardinalTextBoxCustomization() {
        return cardinalValue;
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
