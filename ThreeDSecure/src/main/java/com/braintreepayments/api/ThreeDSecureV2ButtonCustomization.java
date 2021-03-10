package com.braintreepayments.api;

public class ThreeDSecureV2ButtonCustomization {

    private String backgroundColor;
    private int cornerRadius;

    public ThreeDSecureV2ButtonCustomization backgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public ThreeDSecureV2ButtonCustomization cornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }
}
