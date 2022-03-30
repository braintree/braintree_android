package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.cardinalcommerce.shared.userinterfaces.LabelCustomization;

/**
 * Label customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2LabelCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private String textFontName;
    private String textColor;
    private int textFontSize;
    private String headingTextColor;
    private String headingTextFontName;
    private int headingTextFontSize;
    private final LabelCustomization cardinalLabelCustomization = new LabelCustomization();

    public ThreeDSecureV2LabelCustomization() {
    }

    /**
     * @param textFontName Font type for the UI element.
     */
    public void setTextFontName(@Nullable String textFontName) {
        this.textFontName = textFontName;
        cardinalLabelCustomization.setTextFontName(textFontName);
    }

    /**
     * @param textColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setTextColor(@Nullable String textColor) {
        this.textColor = textColor;
        cardinalLabelCustomization.setTextColor(textColor);
    }

    /**
     * @param textFontSize Font size for the UI element.
     */
    public void setTextFontSize(int textFontSize) {
        this.textFontSize = textFontSize;
        cardinalLabelCustomization.setTextFontSize(textFontSize);
    }

    /**
     * @return Font type for the UI element.
     */
    @Nullable
    public String getTextFontName() {
        return cardinalLabelCustomization.getTextFontName();
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getTextColor() {
        return cardinalLabelCustomization.getTextColor();
    }

    /**
     * @return Font size for the UI element.
     */
    public int getTextFontSize() {
        return cardinalLabelCustomization.getTextFontSize();
    }

    /**
     * @param headingTextColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setHeadingTextColor(@Nullable String headingTextColor) {
        this.headingTextColor = headingTextColor;
        cardinalLabelCustomization.setHeadingTextColor(headingTextColor);
    }

    /**
     * @param headingTextFontName Font type for the heading label text.
     */
    public void setHeadingTextFontName(@Nullable String headingTextFontName) {
        this.headingTextFontName = headingTextFontName;
        cardinalLabelCustomization.setHeadingTextFontName(headingTextFontName);
    }

    /**
     * @param headingTextFontSize Font size for the heading label text.
     */
    public void setHeadingTextFontSize(int headingTextFontSize) {
        this.headingTextFontSize = headingTextFontSize;
        cardinalLabelCustomization.setHeadingTextFontSize(headingTextFontSize);
    }

    /**
     * @return Color code in Hex format.
     */
    @Nullable
    public String getHeadingTextColor() {
        return cardinalLabelCustomization.getHeadingTextColor();
    }

    /**
     * @return Font type for the heading label text.
     */
    @Nullable
    public String getHeadingTextFontName() {
        return cardinalLabelCustomization.getHeadingTextFontName();
    }

    /**
     * @return Font size for the heading label text.
     */
    public int getHeadingTextFontSize() {
        return cardinalLabelCustomization.getHeadingTextFontSize();
    }

    LabelCustomization getCardinalLabelCustomization() {
        return cardinalLabelCustomization;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private ThreeDSecureV2LabelCustomization(Parcel in) {
        textFontName = in.readString();
        textColor = in.readString();
        textFontSize = in.readInt();
        headingTextColor = in.readString();
        headingTextFontName = in.readString();
        headingTextFontSize = in.readInt();

        if (textFontName != null) {
            cardinalLabelCustomization.setTextFontName(textFontName);
        }
        if (textColor != null) {
            cardinalLabelCustomization.setTextColor(textColor);
        }
        if (textFontSize != 0) {
            cardinalLabelCustomization.setTextFontSize(textFontSize);
        }
        if (headingTextColor != null) {
            cardinalLabelCustomization.setHeadingTextColor(headingTextColor);
        }
        if (headingTextFontName != null) {
            cardinalLabelCustomization.setHeadingTextFontName(headingTextFontName);
        }
        if (headingTextFontSize != 0) {
            cardinalLabelCustomization.setHeadingTextFontSize(headingTextFontSize);
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(cardinalLabelCustomization.getTextFontName());
        parcel.writeString(cardinalLabelCustomization.getTextColor());
        parcel.writeInt(cardinalLabelCustomization.getTextFontSize());
        parcel.writeString(cardinalLabelCustomization.getHeadingTextColor());
        parcel.writeString(cardinalLabelCustomization.getHeadingTextFontName());
        parcel.writeInt(cardinalLabelCustomization.getHeadingTextFontSize());
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
