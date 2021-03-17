package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.cardinalcommerce.shared.userinterfaces.LabelCustomization;

/**
 * Label customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2LabelCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private String headingTextColor;
    private String headingTextFontName;
    private int headingTextFontSize;
    private final LabelCustomization cardinalLabelCustomization = new LabelCustomization();

    public ThreeDSecureV2LabelCustomization() {
        super.cardinalValue = cardinalLabelCustomization;
    }

    /**
     * @param headingTextColor Color code in Hex format. For example, the color code can be “#999999”.
     */
    public void setHeadingTextColor(String headingTextColor) {
        this.headingTextColor = headingTextColor;
        cardinalLabelCustomization.setHeadingTextColor(headingTextColor);
    }

    /**
     * @param headingTextFontName Font type for the heading label text.
     */
    public void setHeadingTextFontName(String headingTextFontName) {
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
    public String getHeadingTextColor() {
        return headingTextColor;
    }

    /**
     * @return Font type for the heading label text.
     */
    public String getHeadingTextFontName() {
        return headingTextFontName;
    }

    /**
     * @return Font size for the heading label text.
     */
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
        super(in);
        headingTextColor = in.readString();
        headingTextFontName = in.readString();
        headingTextFontSize = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(headingTextColor);
        parcel.writeString(headingTextFontName);
        parcel.writeInt(headingTextFontSize);
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
