package com.braintreepayments.api;

import android.os.Parcel;

import com.cardinalcommerce.shared.models.enums.ButtonType;
import com.cardinalcommerce.shared.userinterfaces.UiCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2UiCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2UiCustomization sut = new ThreeDSecureV2UiCustomization()
                .labelCustomization(new ThreeDSecureV2LabelCustomization())
                .buttonCustomization(new ThreeDSecureV2ButtonCustomization(), ThreeDSecureV2UiCustomization.BUTTON_TYPE_NEXT)
                .textBoxCustomization(new ThreeDSecureV2TextBoxCustomization())
                .toolbarCustomization(new ThreeDSecureV2ToolbarCustomization());

        UiCustomization cardinalUiCustomization = sut.getCardinalUiCustomization();
        assertNotNull(cardinalUiCustomization.getLabelCustomization());
        assertNotNull(cardinalUiCustomization.getButtonCustomization(ButtonType.NEXT));
        assertNotNull(cardinalUiCustomization.getTextBoxCustomization());
        assertNotNull(cardinalUiCustomization.getToolbarCustomization());
    }

    @Test
    public void constructor_defaultsUiCustomizationPropertyToEmptyObject() {
        ThreeDSecureV2UiCustomization sut = new ThreeDSecureV2UiCustomization();
        assertNotNull(sut.getCardinalUiCustomization());
    }

    @Test
    public void writeToParcel() {
        ThreeDSecureV2ButtonCustomization buttonCustomization = (ThreeDSecureV2ButtonCustomization) new ThreeDSecureV2ButtonCustomization()
                .backgroundColor("#FF0000")
                .cornerRadius(5)
                .textColor("#000000")
                .textFontName("Comic Sans")
                .textFontSize(20);

        ThreeDSecureV2LabelCustomization labelCustomization = (ThreeDSecureV2LabelCustomization) new ThreeDSecureV2LabelCustomization()
                .headingTextColor("#FFFFFF")
                .headingTextFontName("Times New Roman")
                .headingTextFontSize(30)
                .textColor("#121212")
                .textFontName("Helvetica")
                .textFontSize(15);

        ThreeDSecureV2TextBoxCustomization textBoxCustomization = (ThreeDSecureV2TextBoxCustomization) new ThreeDSecureV2TextBoxCustomization()
                .borderColor("#FFFFFF")
                .borderWidth(10)
                .cornerRadius(5)
                .textColor("#121212")
                .textFontName("Helvetica")
                .textFontSize(15);

        ThreeDSecureV2ToolbarCustomization toolbarCustomization = (ThreeDSecureV2ToolbarCustomization) new ThreeDSecureV2ToolbarCustomization()
                .backgroundColor("#FFFFFF")
                .buttonText("Button")
                .headerText("Header")
                .textColor("#121212")
                .textFontName("Helvetica")
                .textFontSize(15);

        ThreeDSecureV2UiCustomization customization = new ThreeDSecureV2UiCustomization()
                .buttonCustomization(buttonCustomization, ThreeDSecureV2UiCustomization.BUTTON_TYPE_CONTINUE)
                .labelCustomization(labelCustomization)
                .textBoxCustomization(textBoxCustomization)
                .toolbarCustomization(toolbarCustomization);

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV2UiCustomization actual = (ThreeDSecureV2UiCustomization) ThreeDSecureV2UiCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("#FF0000", actual.getButtonCustomization().getBackgroundColor());
        assertEquals(5, actual.getButtonCustomization().getCornerRadius());
        assertEquals("#000000", actual.getButtonCustomization().getTextColor());
        assertEquals("Comic Sans", actual.getButtonCustomization().getTextFontName());
        assertEquals(20, actual.getButtonCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getLabelCustomization().getHeadingTextColor());
        assertEquals("Times New Roman", actual.getLabelCustomization().getHeadingTextFontName());
        assertEquals(30, actual.getLabelCustomization().getHeadingTextFontSize());
        assertEquals("#121212", actual.getLabelCustomization().getTextColor());
        assertEquals("Helvetica", actual.getLabelCustomization().getTextFontName());
        assertEquals(15, actual.getLabelCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getTextBoxCustomization().getBorderColor());
        assertEquals(10, actual.getTextBoxCustomization().getBorderWidth());
        assertEquals(5, actual.getTextBoxCustomization().getCornerRadius());
        assertEquals("#121212", actual.getTextBoxCustomization().getTextColor());
        assertEquals("Helvetica", actual.getTextBoxCustomization().getTextFontName());
        assertEquals(15, actual.getTextBoxCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getToolbarCustomization().getBackgroundColor());
        assertEquals("Button", actual.getToolbarCustomization().getButtonText());
        assertEquals("Header", actual.getToolbarCustomization().getHeaderText());
        assertEquals("#121212", actual.getToolbarCustomization().getTextColor());
        assertEquals("Helvetica", actual.getToolbarCustomization().getTextFontName());
        assertEquals(15, actual.getToolbarCustomization().getTextFontSize());
    }
}
