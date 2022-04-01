package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization;

/**
 * Toolbar customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2ToolbarCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

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
        cardinalToolbarCustomization.setBackgroundColor(backgroundColor);
    }

    /**
     * @param headerText Text for the header.
     */
    public void setHeaderText(@Nullable String headerText) {
        cardinalToolbarCustomization.setHeaderText(headerText);
    }

    /**
     * @param buttonText Text for the button. For example, “Cancel”.
     */
    public void setButtonText(@Nullable String buttonText) {
        cardinalToolbarCustomization.setButtonText(buttonText);
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getBackgroundColor() {
        return cardinalToolbarCustomization.getBackgroundColor();
    }

    /**
     * @return Text for the header.
     */
    @Nullable
    public String getHeaderText() {
        return cardinalToolbarCustomization.getHeaderText();
    }

    /**
     * @return Text for the button.
     */
    @Nullable
    public String getButtonText() {
        return cardinalToolbarCustomization.getButtonText();
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
        parcel.writeString(cardinalToolbarCustomization.getTextFontName());
        parcel.writeString(cardinalToolbarCustomization.getTextColor());
        parcel.writeInt(cardinalToolbarCustomization.getTextFontSize());
        parcel.writeString(cardinalToolbarCustomization.getBackgroundColor());
        parcel.writeString(cardinalToolbarCustomization.getHeaderText());
        parcel.writeString(cardinalToolbarCustomization.getButtonText());
    }

    private ThreeDSecureV2ToolbarCustomization(Parcel in) {
        String textFontName = in.readString();
        String textColor = in.readString();
        int textFontSize = in.readInt();
        String backgroundColor = in.readString();
        String headerText = in.readString();
        String buttonText = in.readString();

        if (textFontName != null) {
            cardinalToolbarCustomization.setTextFontName(textFontName);
        }
        if (textColor != null) {
            cardinalToolbarCustomization.setTextColor(textColor);
        }
        if (textFontSize != 0) {
            cardinalToolbarCustomization.setTextFontSize(textFontSize);
        }
        if (backgroundColor != null) {
            cardinalToolbarCustomization.setBackgroundColor(backgroundColor);
        }
        if (headerText != null) {
            cardinalToolbarCustomization.setHeaderText(headerText);
        }
        if (buttonText != null) {
            cardinalToolbarCustomization.setButtonText(buttonText);
        }
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
