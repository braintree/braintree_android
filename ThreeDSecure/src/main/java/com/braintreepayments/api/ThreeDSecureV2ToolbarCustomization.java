package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

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
    }

    /**
     * @param textFontName Font type for the UI element.
     */
    public void setTextFontName(@Nullable String textFontName) {
        cardinalToolbarCustomization.setTextFontName(textFontName);
    }

    /**
     * @param textColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setTextColor(@Nullable String textColor) {
        cardinalToolbarCustomization.setTextColor(textColor);
    }

    /**
     * @param textFontSize Font size for the UI element.
     */
    public void setTextFontSize(int textFontSize) {
        cardinalToolbarCustomization.setTextFontSize(textFontSize);
    }

    /**
     * @return Font type for the UI element.
     */
    @Nullable
    public String getTextFontName() {
        return cardinalToolbarCustomization.getTextFontName();
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getTextColor() {
        return cardinalToolbarCustomization.getTextColor();
    }

    /**
     * @return Font size for the UI element.
     */
    public int getTextFontSize() {
        return cardinalToolbarCustomization.getTextFontSize();
    }

    /**
     * @param backgroundColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setBackgroundColor(@Nullable String backgroundColor) {
        this.backgroundColor = backgroundColor;
        cardinalToolbarCustomization.setBackgroundColor(backgroundColor);
    }

    /**
     * @param headerText Text for the header.
     */
    public void setHeaderText(@Nullable String headerText) {
        this.headerText = headerText;
        cardinalToolbarCustomization.setHeaderText(headerText);
    }

    /**
     * @param buttonText Text for the button. For example, “Cancel”.
     */
    public void setButtonText(@Nullable String buttonText) {
        this.buttonText = buttonText;
        cardinalToolbarCustomization.setButtonText(buttonText);
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @return Text for the header.
     */
    @Nullable
    public String getHeaderText() {
        return headerText;
    }

    /**
     * @return Text for the button.
     */
    @Nullable
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
