package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization;

public class ThreeDSecureV2ToolbarCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private String backgroundColor;
    private String headerText;
    private String buttonText;
    private final ToolbarCustomization cardinalToolbarCustomization = new ToolbarCustomization();

    public ThreeDSecureV2ToolbarCustomization() {
        super.cardinalValue = cardinalToolbarCustomization;
    }

    public ThreeDSecureV2ToolbarCustomization backgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        cardinalToolbarCustomization.setBackgroundColor(backgroundColor);
        return this;
    }

    public ThreeDSecureV2ToolbarCustomization headerText(String headerText) {
        this.headerText = headerText;
        cardinalToolbarCustomization.setHeaderText(headerText);
        return this;
    }

    public ThreeDSecureV2ToolbarCustomization buttonText(String buttonText) {
        this.buttonText = buttonText;
        cardinalToolbarCustomization.setButtonText(buttonText);
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getHeaderText() {
        return headerText;
    }

    public String getButtonText() {
        return buttonText;
    }

    ToolbarCustomization getCardinalToolbarCustomization() {
        return cardinalToolbarCustomization;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(backgroundColor);
        parcel.writeString(headerText);
        parcel.writeString(buttonText);
    }

    private ThreeDSecureV2ToolbarCustomization(Parcel in) {
        backgroundColor = in.readString();
        headerText = in.readString();
        buttonText = in.readString();
    }

    public static final Creator<ThreeDSecureV2ToolbarCustomization> CREATOR = new Creator<ThreeDSecureV2ToolbarCustomization>() {
        @Override
        public ThreeDSecureV2ToolbarCustomization createFromParcel(Parcel in) {
            return new ThreeDSecureV2ToolbarCustomization(in);
        }

        @Override
        public ThreeDSecureV2ToolbarCustomization[] newArray(int size) {
            return new ThreeDSecureV2ToolbarCustomization[size];
        }
    };
}
