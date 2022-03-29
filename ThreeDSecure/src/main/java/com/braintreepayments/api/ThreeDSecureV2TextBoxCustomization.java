package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization;

/**
 * Text box customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2TextBoxCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private int borderWidth;
    private String borderColor;
    private int cornerRadius;
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
        this.borderWidth = borderWidth;
        cardinalTextBoxCustomization.setBorderWidth(borderWidth);
    }

    /**
     * @param borderColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setBorderColor(@Nullable String borderColor) {
        this.borderColor = borderColor;
        cardinalTextBoxCustomization.setBorderColor(borderColor);
    }

    /**
     * @param cornerRadius Radius (integer value) for the text box corners.
     */
    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        cardinalTextBoxCustomization.setCornerRadius(cornerRadius);
    }

    /**
     * @return Width (integer value) of the text box border.
     */
    public int getBorderWidth() {
        return borderWidth;
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getBorderColor() {
        return borderColor;
    }

    /**
     * @return Radius (integer value) for the text box corners.
     */
    public int getCornerRadius() {
        return cornerRadius;
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
        super.writeToParcel(parcel, i);
        parcel.writeInt(borderWidth);
        parcel.writeString(borderColor);
        parcel.writeInt(cornerRadius);
    }

    private ThreeDSecureV2TextBoxCustomization(Parcel in) {
        super(in);
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
