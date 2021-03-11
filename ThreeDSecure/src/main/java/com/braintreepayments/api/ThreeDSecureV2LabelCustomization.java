package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.cardinalcommerce.shared.userinterfaces.LabelCustomization;

public class ThreeDSecureV2LabelCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private String headingTextColor;
    private String headingTextFontName;
    private int headingTextFontSize;
    private final LabelCustomization cardinalLabelCustomization = new LabelCustomization();

    public ThreeDSecureV2LabelCustomization() {
        super.cardinalValue = cardinalLabelCustomization;
    }

    public ThreeDSecureV2LabelCustomization headingTextColor(String headingTextColor) {
        this.headingTextColor = headingTextColor;
        cardinalLabelCustomization.setHeadingTextColor(headingTextColor);
        return this;
    }

    public ThreeDSecureV2LabelCustomization headingTextFontName(String headingTextFontName) {
        this.headingTextFontName = headingTextFontName;
        cardinalLabelCustomization.setHeadingTextFontName(headingTextFontName);
        return this;
    }

    public ThreeDSecureV2LabelCustomization headingTextFontSize(int headingTextFontSize) {
        this.headingTextFontSize = headingTextFontSize;
        cardinalLabelCustomization.setHeadingTextFontSize(headingTextFontSize);
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

    LabelCustomization getCardinalLabelCustomization() {
        return cardinalLabelCustomization;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private ThreeDSecureV2LabelCustomization(Parcel in) {
        headingTextColor = in.readString();
        headingTextFontName = in.readString();
        headingTextFontSize = in.readInt();
        cardinalValue = (LabelCustomization) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(headingTextColor);
        parcel.writeString(headingTextFontName);
        parcel.writeInt(headingTextFontSize);
        parcel.writeSerializable(cardinalValue);
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
