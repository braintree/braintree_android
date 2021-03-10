package com.braintreepayments.api;

public class ThreeDSecureV2TextBoxCustomization {

    private int borderWidth;
    private String borderColor;
    private int cornerRadius;

    public ThreeDSecureV2TextBoxCustomization borderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        return this;
    }

    public ThreeDSecureV2TextBoxCustomization borderColor(String borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    public ThreeDSecureV2TextBoxCustomization cornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }
}
