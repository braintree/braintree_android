package com.braintreepayments.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.cardinalcommerce.shared.userinterfaces.ButtonCustomization;

/**
 * Button customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2ButtonCustomization extends ThreeDSecureV2BaseCustomization implements Parcelable {

    private String backgroundColor;
    private int cornerRadius;
    private ButtonCustomization cardinalButtonCustomization = new ButtonCustomization();

    public ThreeDSecureV2ButtonCustomization() {
        super.cardinalValue = cardinalButtonCustomization;
    }

    /**
     * @param backgroundColor Color code in Hex format. For example, the color code can be “#999999”.
     * @return {@link ThreeDSecureV2ButtonCustomization}
     */
    public ThreeDSecureV2ButtonCustomization backgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        cardinalButtonCustomization.setBackgroundColor(backgroundColor);
        return this;
    }

    /**
     * @param cornerRadius Radius (integer value) for the button corners.
     * @return {@link ThreeDSecureV2ButtonCustomization}
     */
    public ThreeDSecureV2ButtonCustomization cornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        cardinalButtonCustomization.setCornerRadius(cornerRadius);
        return this;
    }

    /**
     * @return Color code in Hex format.
     */
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(backgroundColor);
        dest.writeInt(cornerRadius);
        dest.writeSerializable(cardinalButtonCustomization);
    }

    private ThreeDSecureV2ButtonCustomization(Parcel in) {
        backgroundColor = in.readString();
        cornerRadius = in.readInt();
        cardinalButtonCustomization = (ButtonCustomization) in.readSerializable();
    }
}
