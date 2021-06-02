package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cardinalcommerce.shared.userinterfaces.Customization;

/**
 * Base customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2BaseCustomization implements Parcelable {

    private String textFontName;
    private String textColor;
    private int textFontSize;
    protected Customization cardinalValue;

    ThreeDSecureV2BaseCustomization() {}

    /**
     * @param textFontName Font type for the UI element.
     */
    public void setTextFontName(@NonNull String textFontName) {
        this.textFontName = textFontName;
        cardinalValue.setTextFontName(textFontName);
    }

    /**
     * @param textColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setTextColor(@NonNull String textColor) {
        this.textColor = textColor;
        cardinalValue.setTextColor(textColor);
    }

    /**
     * @param textFontSize Font size for the UI element.
     */
    public void setTextFontSize(int textFontSize) {
        this.textFontSize = textFontSize;
        cardinalValue.setTextFontSize(textFontSize);
    }

    /**
     * @return Font type for the UI element.
     */
    @Nullable
    public String getTextFontName() {
        return textFontName;
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getTextColor() {
        return textColor;
    }

    /**
     * @return Font size for the UI element.
     */
    public int getTextFontSize() {
        return textFontSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(textFontName);
        parcel.writeString(textColor);
        parcel.writeInt(textFontSize);
    }

    protected ThreeDSecureV2BaseCustomization(Parcel in) {
        textFontName = in.readString();
        textColor = in.readString();
        textFontSize = in.readInt();
    }

    public static final Creator<ThreeDSecureV2BaseCustomization> CREATOR = new Creator<ThreeDSecureV2BaseCustomization>() {
        @Override
        public ThreeDSecureV2BaseCustomization createFromParcel(Parcel in) {
            return new ThreeDSecureV2BaseCustomization(in);
        }

        @Override
        public ThreeDSecureV2BaseCustomization[] newArray(int size) {
            return new ThreeDSecureV2BaseCustomization[size];
        }
    };
}
