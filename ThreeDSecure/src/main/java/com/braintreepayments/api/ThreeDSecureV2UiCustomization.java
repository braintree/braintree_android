package com.braintreepayments.api;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;

import com.cardinalcommerce.shared.models.enums.ButtonType;
import com.cardinalcommerce.shared.userinterfaces.ButtonCustomization;
import com.cardinalcommerce.shared.userinterfaces.LabelCustomization;
import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization;
import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization;
import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ThreeDSecureV2UiCustomization implements Parcelable {

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
    private @ThreeDSecureV2ButtonType int buttonType;
    private UiCustomization uiCustomization = new UiCustomization();

    public ThreeDSecureV2UiCustomization() {}

    public ThreeDSecureV2UiCustomization buttonCustomization(ThreeDSecureV2ButtonCustomization buttonCustomization, @ThreeDSecureV2ButtonType int buttonType) {
        this.buttonCustomization = buttonCustomization;
        this.buttonType = buttonType;

        ButtonCustomization cardinalButtonCustomization = new ButtonCustomization();
        cardinalButtonCustomization.setBackgroundColor(buttonCustomization.getBackgroundColor());
        cardinalButtonCustomization.setCornerRadius(buttonCustomization.getCornerRadius());
        cardinalButtonCustomization.setTextColor(buttonCustomization.getTextColor());
        cardinalButtonCustomization.setTextFontName(buttonCustomization.getTextFontName());
        cardinalButtonCustomization.setTextFontSize(buttonCustomization.getTextFontSize());
        uiCustomization.setButtonCustomization(cardinalButtonCustomization, getCardinalButtonType(buttonType));

        return this;
    }

    public ThreeDSecureV2UiCustomization labelCustomization(ThreeDSecureV2LabelCustomization labelCustomization) {
        this.labelCustomization = labelCustomization;

        LabelCustomization cardinalLabelCustomization = new LabelCustomization();
        cardinalLabelCustomization.setHeadingTextColor(labelCustomization.getHeadingTextColor());
        cardinalLabelCustomization.setHeadingTextFontName(labelCustomization.getHeadingTextFontName());
        cardinalLabelCustomization.setHeadingTextFontSize(labelCustomization.getHeadingTextFontSize());
        cardinalLabelCustomization.setTextColor(labelCustomization.getTextColor());
        cardinalLabelCustomization.setTextFontName(labelCustomization.getTextFontName());
        cardinalLabelCustomization.setTextFontSize(labelCustomization.getTextFontSize());
        uiCustomization.setLabelCustomization(cardinalLabelCustomization);

        return this;
    }

    public ThreeDSecureV2UiCustomization textBoxCustomization(ThreeDSecureV2TextBoxCustomization textBoxCustomization) {
        this.textBoxCustomization = textBoxCustomization;

        TextBoxCustomization cardinalTextBoxCustomization = new TextBoxCustomization();
        cardinalTextBoxCustomization.setBorderColor(textBoxCustomization.getBorderColor());
        cardinalTextBoxCustomization.setBorderWidth(textBoxCustomization.getBorderWidth());
        cardinalTextBoxCustomization.setCornerRadius(textBoxCustomization.getCornerRadius());
        cardinalTextBoxCustomization.setTextColor(textBoxCustomization.getTextColor());
        cardinalTextBoxCustomization.setTextFontName(textBoxCustomization.getTextFontName());
        cardinalTextBoxCustomization.setTextFontSize(textBoxCustomization.getTextFontSize());
        uiCustomization.setTextBoxCustomization(cardinalTextBoxCustomization);

        return this;
    }

    public ThreeDSecureV2UiCustomization toolbarCustomization(ThreeDSecureV2ToolbarCustomization toolbarCustomization) {
        this.toolbarCustomization = toolbarCustomization;

        ToolbarCustomization cardinalToolbarCustomization = new ToolbarCustomization();
        cardinalToolbarCustomization.setBackgroundColor(toolbarCustomization.getBackgroundColor());
        cardinalToolbarCustomization.setButtonText(toolbarCustomization.getButtonText());
        cardinalToolbarCustomization.setHeaderText(toolbarCustomization.getHeaderText());
        if (toolbarCustomization.getTextColor() != null) {
            cardinalToolbarCustomization.setTextColor(toolbarCustomization.getTextColor());
        }
        if (toolbarCustomization.getTextFontName() != null) {
            cardinalToolbarCustomization.setTextFontName(toolbarCustomization.getTextFontName());
            cardinalToolbarCustomization.setTextFontSize(toolbarCustomization.getTextFontSize());
        }

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

    UiCustomization getUiCustomization() {
        return uiCustomization;
    }

    private ButtonType getCardinalButtonType(@ThreeDSecureV2ButtonType int buttonType) {
        switch (buttonType) {
            case VERIFY:
                return ButtonType.VERIFY;
            case CONTINUE:
                return ButtonType.CONTINUE;
            case NEXT:
                return ButtonType.NEXT;
            case CANCEL:
                return ButtonType.CANCEL;
            case RESEND:
                return ButtonType.RESEND;
            default:
                return null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(buttonCustomization, i);
        parcel.writeParcelable(labelCustomization, i);
        parcel.writeParcelable(textBoxCustomization, i);
        parcel.writeParcelable(toolbarCustomization, i);
        parcel.writeInt(buttonType);
    }

    private ThreeDSecureV2UiCustomization(Parcel in) {
        buttonCustomization = in.readParcelable(ThreeDSecureV2ButtonCustomization.class.getClassLoader());
        labelCustomization = in.readParcelable(ThreeDSecureV2LabelCustomization.class.getClassLoader());
        textBoxCustomization = in.readParcelable(ThreeDSecureV2TextBoxCustomization.class.getClassLoader());
        toolbarCustomization = in.readParcelable(ThreeDSecureV2ToolbarCustomization.class.getClassLoader());
        buttonType = in.readInt();
    }

    public static final Creator<ThreeDSecureV2UiCustomization> CREATOR = new Creator<ThreeDSecureV2UiCustomization>() {
        @Override
        public ThreeDSecureV2UiCustomization createFromParcel(Parcel in) {
            return new ThreeDSecureV2UiCustomization(in);
        }

        @Override
        public ThreeDSecureV2UiCustomization[] newArray(int size) {
            return new ThreeDSecureV2UiCustomization[size];
        }
    };
}
