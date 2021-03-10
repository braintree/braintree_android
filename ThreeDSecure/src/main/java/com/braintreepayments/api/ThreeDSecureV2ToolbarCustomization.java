package com.braintreepayments.api;

public class ThreeDSecureV2ToolbarCustomization {

    private String backgroundColor;
    private String headerText;
    private String buttonText;

    public ThreeDSecureV2ToolbarCustomization backgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public ThreeDSecureV2ToolbarCustomization headerText(String headerText) {
        this.headerText = headerText;
        return this;
    }

    public ThreeDSecureV2ToolbarCustomization buttonText(String buttonText) {
        this.buttonText = buttonText;
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getHeaderText() {
        return headerText;
    }

    public String getButtonText() {
        return buttonText;
    }
}
