package com.braintreepayments.api;

public class ThreeDSecureV2LabelCustomization {

    private String headingTextColor;
    private String headingTextFontName;
    private int headingTextFontSize;

    public ThreeDSecureV2LabelCustomization headingTextColor(String headingTextColor) {
        this.headingTextColor = headingTextColor;
        return this;
    }

    public ThreeDSecureV2LabelCustomization headingTextFontName(String headingTextFontName) {
        this.headingTextFontName = headingTextFontName;
        return this;
    }

    public ThreeDSecureV2LabelCustomization headingTextFontSize(int headingTextFontSize) {
        this.headingTextFontSize = headingTextFontSize;
        return this;
    }

    public String getHeadingTextColor() {
        return headingTextColor;
    }

    public String getHeadingTextFontName() {
        return headingTextFontName;
    }

    public int getHeadingTextFontSize() {
        return headingTextFontSize;
    }
}
