package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization;

/**
 * Toolbar customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2ToolbarCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private String backgroundColor;
    private String headerText;
    private String buttonText;
    private final ToolbarCustomization cardinalToolbarCustomization = new ToolbarCustomization();

    public ThreeDSecureV2ToolbarCustomization() {
        super.cardinalValue = cardinalToolbarCustomization;
    }

    /**
     * @param backgroundColor Color code in Hex format. For example, the color code can be “#999999”.
     * @return {@link ThreeDSecureV2ToolbarCustomization}
     */
    public ThreeDSecureV2ToolbarCustomization backgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        cardinalToolbarCustomization.setBackgroundColor(backgroundColor);
        return this;
    }

    /**
     * @param headerText Text for the header.
     * @return {@link ThreeDSecureV2ToolbarCustomization}
     */
    public ThreeDSecureV2ToolbarCustomization headerText(String headerText) {
        this.headerText = headerText;
        cardinalToolbarCustomization.setHeaderText(headerText);
        return this;
    }

    /**
     * @param buttonText Text for the button. For example, “Cancel”.
     * @return {@link ThreeDSecureV2ToolbarCustomization}
     */
    public ThreeDSecureV2ToolbarCustomization buttonText(String buttonText) {
        this.buttonText = buttonText;
        cardinalToolbarCustomization.setButtonText(buttonText);
        return this;
    }

    /**
     * @return Color code in Hex format.
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @return Text for the header.
     */
    public String getHeaderText() {
        return headerText;
    }

    /**
     * @return Text for the button.
     */
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
        super.writeToParcel(parcel, i);
        parcel.writeString(backgroundColor);
        parcel.writeString(headerText);
        parcel.writeString(buttonText);
    }

    private ThreeDSecureV2ToolbarCustomization(Parcel in) {
        super(in);
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
