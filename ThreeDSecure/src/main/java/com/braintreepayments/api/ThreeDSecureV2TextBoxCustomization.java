package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization;

/**
 * Text box customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2TextBoxCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private final TextBoxCustomization cardinalTextBoxCustomization = new TextBoxCustomization();

    public ThreeDSecureV2TextBoxCustomization() {
    }

    /**
     * @param textFontName Font type for the UI element.
     */
    public void setTextFontName(@Nullable String textFontName) {
        cardinalTextBoxCustomization.setTextFontName(textFontName);
    }

    /**
     * @param textColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setTextColor(@Nullable String textColor) {
        cardinalTextBoxCustomization.setTextColor(textColor);
    }

    /**
     * @param textFontSize Font size for the UI element.
     */
    public void setTextFontSize(int textFontSize) {
        cardinalTextBoxCustomization.setTextFontSize(textFontSize);
    }

    /**
     * @return Font type for the UI element.
     */
    @Nullable
    public String getTextFontName() {
        return cardinalTextBoxCustomization.getTextFontName();
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getTextColor() {
        return cardinalTextBoxCustomization.getTextColor();
    }

    /**
     * @return Font size for the UI element.
     */
    public int getTextFontSize() {
        return cardinalTextBoxCustomization.getTextFontSize();
    }

    /**
     * @param borderWidth Width (integer value) of the text box border.
     */
    public void setBorderWidth(int borderWidth) {
        cardinalTextBoxCustomization.setBorderWidth(borderWidth);
    }

    /**
     * @param borderColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setBorderColor(@Nullable String borderColor) {
        cardinalTextBoxCustomization.setBorderColor(borderColor);
    }

    /**
     * @param cornerRadius Radius (integer value) for the text box corners.
     */
    public void setCornerRadius(int cornerRadius) {
        cardinalTextBoxCustomization.setCornerRadius(cornerRadius);
    }

    /**
     * @return Width (integer value) of the text box border.
     */
    public int getBorderWidth() {
        return cardinalTextBoxCustomization.getBorderWidth();
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getBorderColor() {
        return cardinalTextBoxCustomization.getBorderColor();
    }

    /**
     * @return Radius (integer value) for the text box corners.
     */
    public int getCornerRadius() {
        return cardinalTextBoxCustomization.getCornerRadius();
    }

    TextBoxCustomization getCardinalTextBoxCustomization() {
        return cardinalTextBoxCustomization;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(cardinalTextBoxCustomization.getTextFontName());
        parcel.writeString(cardinalTextBoxCustomization.getTextColor());
        parcel.writeInt(cardinalTextBoxCustomization.getTextFontSize());
        parcel.writeInt(cardinalTextBoxCustomization.getBorderWidth());
        parcel.writeString(cardinalTextBoxCustomization.getBorderColor());
        parcel.writeInt(cardinalTextBoxCustomization.getCornerRadius());
    }

    private ThreeDSecureV2TextBoxCustomization(Parcel in) {
        String textFontName = in.readString();
        String textColor = in.readString();
        int textFontSize = in.readInt();
        int borderWidth = in.readInt();
        String borderColor = in.readString();
        int cornerRadius = in.readInt();

        if (textFontName != null) {
            cardinalTextBoxCustomization.setTextFontName(textFontName);
        }
        if (textColor != null) {
            cardinalTextBoxCustomization.setTextColor(textColor);
        }
        if (textFontSize != 0) {
            cardinalTextBoxCustomization.setTextFontSize(textFontSize);
        }
        if (borderWidth != 0) {
            cardinalTextBoxCustomization.setBorderWidth(borderWidth);
        }
        if (borderColor != null) {
            cardinalTextBoxCustomization.setBorderColor(borderColor);
        }
        if (cornerRadius != 0) {
            cardinalTextBoxCustomization.setCornerRadius(cornerRadius);
        }
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
