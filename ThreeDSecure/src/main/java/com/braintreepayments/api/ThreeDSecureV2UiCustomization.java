package com.braintreepayments.api;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;

import com.cardinalcommerce.shared.models.enums.ButtonType;
import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * UI customization options for 3D Secure 2 flows.
 */
public class ThreeDSecureV2UiCustomization implements Parcelable {

    /**
     * Button types that can be customized in 3D Secure 2 flows.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BUTTON_TYPE_VERIFY, BUTTON_TYPE_CONTINUE, BUTTON_TYPE_NEXT, BUTTON_TYPE_CANCEL, BUTTON_TYPE_RESEND})
    @interface ThreeDSecureV2ButtonType {}
    public static final int BUTTON_TYPE_VERIFY = 0;
    public static final int BUTTON_TYPE_CONTINUE = 1;
    public static final int BUTTON_TYPE_NEXT = 2;
    public static final int BUTTON_TYPE_CANCEL = 3;
    public static final int BUTTON_TYPE_RESEND = 4;

    private ThreeDSecureV2ButtonCustomization buttonCustomization;
    private ThreeDSecureV2LabelCustomization labelCustomization;
    private ThreeDSecureV2TextBoxCustomization textBoxCustomization;
    private ThreeDSecureV2ToolbarCustomization toolbarCustomization;
    private @ThreeDSecureV2ButtonType int buttonType;
    private UiCustomization cardinalValue = new UiCustomization();

    public ThreeDSecureV2UiCustomization() {}

    /**
     * Set button customization options for 3D Secure 2 flows.
     * @param buttonCustomization {@link ThreeDSecureV2ButtonCustomization}
     * @param buttonType Button type
     * @return {@link ThreeDSecureV2UiCustomization}
     */
    public ThreeDSecureV2UiCustomization buttonCustomization(ThreeDSecureV2ButtonCustomization buttonCustomization, @ThreeDSecureV2ButtonType int buttonType) {
        this.buttonCustomization = buttonCustomization;
        this.buttonType = buttonType;
        cardinalValue.setButtonCustomization(buttonCustomization.getCardinalButtonCustomization(), getCardinalButtonType(buttonType));
        return this;
    }

    /**
     * Label customization options for 3D Secure 2 flows.
     * @param labelCustomization {@link ThreeDSecureV2LabelCustomization}
     * @return {@link ThreeDSecureV2UiCustomization}
     */
    public ThreeDSecureV2UiCustomization labelCustomization(ThreeDSecureV2LabelCustomization labelCustomization) {
        this.labelCustomization = labelCustomization;
        cardinalValue.setLabelCustomization(labelCustomization.getCardinalLabelCustomization());
        return this;
    }

    /**
     * Text box customization options for 3D Secure 2 flows.
     * @param textBoxCustomization {@link ThreeDSecureV2TextBoxCustomization}
     * @return {@link ThreeDSecureV2UiCustomization}
     */
    public ThreeDSecureV2UiCustomization textBoxCustomization(ThreeDSecureV2TextBoxCustomization textBoxCustomization) {
        this.textBoxCustomization = textBoxCustomization;
        cardinalValue.setTextBoxCustomization(textBoxCustomization.getCardinalTextBoxCustomization());
        return this;
    }

    /**
     * Toolbar customization options for 3D Secure 2 flows.
     * @param toolbarCustomization {@link ThreeDSecureV2ToolbarCustomization}
     * @return {@link ThreeDSecureV2UiCustomization}
     */
    public ThreeDSecureV2UiCustomization toolbarCustomization(ThreeDSecureV2ToolbarCustomization toolbarCustomization) {
        this.toolbarCustomization = toolbarCustomization;
        cardinalValue.setToolbarCustomization(toolbarCustomization.getCardinalToolbarCustomization());
        return this;
    }

    /**
     * @return {@link ThreeDSecureV2ButtonCustomization}
     */
    public ThreeDSecureV2ButtonCustomization getButtonCustomization() {
        return buttonCustomization;
    }

    /**
     * @return {@link ThreeDSecureV2LabelCustomization}
     */
    public ThreeDSecureV2LabelCustomization getLabelCustomization() {
        return labelCustomization;
    }

    /**
     * @return {@link ThreeDSecureV2TextBoxCustomization}
     */
    public ThreeDSecureV2TextBoxCustomization getTextBoxCustomization() {
        return textBoxCustomization;
    }

    /**
     * @return {@link ThreeDSecureV2ToolbarCustomization}
     */
    public ThreeDSecureV2ToolbarCustomization getToolbarCustomization() {
        return toolbarCustomization;
    }

    UiCustomization getCardinalUiCustomization() {
        return cardinalValue;
    }

    private ButtonType getCardinalButtonType(@ThreeDSecureV2ButtonType int buttonType) {
        switch (buttonType) {
            case BUTTON_TYPE_VERIFY:
                return ButtonType.VERIFY;
            case BUTTON_TYPE_CONTINUE:
                return ButtonType.CONTINUE;
            case BUTTON_TYPE_NEXT:
                return ButtonType.NEXT;
            case BUTTON_TYPE_CANCEL:
                return ButtonType.CANCEL;
            case BUTTON_TYPE_RESEND:
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
        parcel.writeSerializable(cardinalValue);
    }

    private ThreeDSecureV2UiCustomization(Parcel in) {
        buttonCustomization = in.readParcelable(ThreeDSecureV2ButtonCustomization.class.getClassLoader());
        labelCustomization = in.readParcelable(ThreeDSecureV2LabelCustomization.class.getClassLoader());
        textBoxCustomization = in.readParcelable(ThreeDSecureV2TextBoxCustomization.class.getClassLoader());
        toolbarCustomization = in.readParcelable(ThreeDSecureV2ToolbarCustomization.class.getClassLoader());
        buttonType = in.readInt();
        cardinalValue = (UiCustomization) in.readSerializable();
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
