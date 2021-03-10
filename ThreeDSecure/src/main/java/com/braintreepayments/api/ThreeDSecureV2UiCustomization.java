package com.braintreepayments.api;

public class ThreeDSecureV2UiCustomization {

    private ThreeDSecureV2ButtonCustomization buttonCustomization;
    private ThreeDSecureV2LabelCustomization labelCustomization;
    private ThreeDSecureV2TextBoxCustomization textBoxCustomization;
    private ThreeDSecureV2ToolbarCustomization toolbarCustomization;

    public void setButtonCustomization(ThreeDSecureV2ButtonCustomization buttonCustomization) {
        this.buttonCustomization = buttonCustomization;
    }

    public void setLabelCustomization(ThreeDSecureV2LabelCustomization labelCustomization) {
        this.labelCustomization = labelCustomization;
    }

    public void setTextBoxCustomization(ThreeDSecureV2TextBoxCustomization textBoxCustomization) {
        this.textBoxCustomization = textBoxCustomization;
    }

    public void setToolbarCustomization(ThreeDSecureV2ToolbarCustomization toolbarCustomization) {
        this.toolbarCustomization = toolbarCustomization;
    }

    public ThreeDSecureV2ButtonCustomization getButtonCustomization() {
        return buttonCustomization;
    }

    public ThreeDSecureV2LabelCustomization getLabelCustomization() {
        return labelCustomization;
    }

    public ThreeDSecureV2TextBoxCustomization getTextBoxCustomization() {
        return textBoxCustomization;
    }

    public ThreeDSecureV2ToolbarCustomization getToolbarCustomization() {
        return toolbarCustomization;
    }
}
