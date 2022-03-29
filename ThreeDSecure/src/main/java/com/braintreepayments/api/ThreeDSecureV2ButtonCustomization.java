package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.cardinalcommerce.shared.userinterfaces.ButtonCustomization;

/**
 * Button customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2ButtonCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private String backgroundColor;
    private int cornerRadius;
    private final ButtonCustomization cardinalButtonCustomization = new ButtonCustomization();

    public ThreeDSecureV2ButtonCustomization() {
    }

    /**
     * @param textFontName Font type for the UI element.
     */
    public void setTextFontName(@Nullable String textFontName) {
        cardinalButtonCustomization.setTextFontName(textFontName);
    }

    /**
     * @param textColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setTextColor(@Nullable String textColor) {
        cardinalButtonCustomization.setTextColor(textColor);
    }

    /**
     * @param textFontSize Font size for the UI element.
     */
    public void setTextFontSize(int textFontSize) {
        cardinalButtonCustomization.setTextFontSize(textFontSize);
    }

    /**
     * @return Font type for the UI element.
     */
    @Nullable
    public String getTextFontName() {
        return cardinalButtonCustomization.getTextFontName();
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getTextColor() {
        return cardinalButtonCustomization.getTextColor();
    }

    /**
     * @return Font size for the UI element.
     */
    public int getTextFontSize() {
        return cardinalButtonCustomization.getTextFontSize();
    }

    /**
     * @param backgroundColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setBackgroundColor(@Nullable String backgroundColor) {
        this.backgroundColor = backgroundColor;
        cardinalButtonCustomization.setBackgroundColor(backgroundColor);
    }

    /**
     * @param cornerRadius Radius (integer value) for the button corners.
     */
    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        cardinalButtonCustomization.setCornerRadius(cornerRadius);
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @return Radius (integer value) for the button corners.
     */
    public int getCornerRadius() {
        return cornerRadius;
    }

    ButtonCustomization getCardinalButtonCustomization() {
        return cardinalButtonCustomization;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(backgroundColor);
        parcel.writeInt(cornerRadius);
    }

    protected ThreeDSecureV2ButtonCustomization(Parcel in) {
        super(in);
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
