package com.braintreepayments.api;

import androidx.annotation.IntDef;

import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ThreeDSecureV2UiCustomization {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({VERIFY, CONTINUE, NEXT, CANCEL, RESEND})
    @interface ThreeDSecureV2ButtonType {}
    public static final int VERIFY = 0;
    public static final int CONTINUE = 1;
    public static final int NEXT = 2;
    public static final int CANCEL = 3;
    public static final int RESEND = 4;

    private ThreeDSecureV2ButtonCustomization buttonCustomization;
    private ThreeDSecureV2LabelCustomization labelCustomization;
    private ThreeDSecureV2TextBoxCustomization textBoxCustomization;
    private ThreeDSecureV2ToolbarCustomization toolbarCustomization;
    private UiCustomization uiCustomization = new UiCustomization();

    public ThreeDSecureV2UiCustomization buttonCustomization(ThreeDSecureV2ButtonCustomization buttonCustomization) {
        this.buttonCustomization = buttonCustomization;
        return this;
    }

    public ThreeDSecureV2UiCustomization labelCustomization(ThreeDSecureV2LabelCustomization labelCustomization) {
        this.labelCustomization = labelCustomization;
        return this;
    }

    public ThreeDSecureV2UiCustomization textBoxCustomization(ThreeDSecureV2TextBoxCustomization textBoxCustomization) {
        this.textBoxCustomization = textBoxCustomization;
        return this;
    }

    public ThreeDSecureV2UiCustomization toolbarCustomization(ThreeDSecureV2ToolbarCustomization toolbarCustomization) {
        this.toolbarCustomization = toolbarCustomization;
        return this;
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
