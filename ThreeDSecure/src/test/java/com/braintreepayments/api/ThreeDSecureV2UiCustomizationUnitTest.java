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
        ThreeDSecureV2UiCustomization sut = new ThreeDSecureV2UiCustomization();
        sut.labelCustomization(new ThreeDSecureV2LabelCustomization());
        sut.buttonCustomization(new ThreeDSecureV2ButtonCustomization(), ThreeDSecureV2UiCustomization.BUTTON_TYPE_NEXT);
        sut.textBoxCustomization(new ThreeDSecureV2TextBoxCustomization());
        sut.toolbarCustomization(new ThreeDSecureV2ToolbarCustomization());

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
        ThreeDSecureV2ButtonCustomization buttonCustomization = new ThreeDSecureV2ButtonCustomization();
        buttonCustomization.backgroundColor("#FF0000");
        buttonCustomization.cornerRadius(5);
        buttonCustomization.textColor("#000000");
        buttonCustomization.textFontName("Comic Sans");
        buttonCustomization.textFontSize(20);

        ThreeDSecureV2LabelCustomization labelCustomization = new ThreeDSecureV2LabelCustomization();
        labelCustomization.headingTextColor("#FFFFFF");
        labelCustomization.headingTextFontName("Times New Roman");
        labelCustomization.headingTextFontSize(30);
        labelCustomization.textColor("#121212");
        labelCustomization.textFontName("Helvetica");
        labelCustomization.textFontSize(15);

        ThreeDSecureV2TextBoxCustomization textBoxCustomization = new ThreeDSecureV2TextBoxCustomization();
        textBoxCustomization.borderColor("#FFFFFF");
        textBoxCustomization.borderWidth(10);
        textBoxCustomization.cornerRadius(5);
        textBoxCustomization.textColor("#121212");
        textBoxCustomization.textFontName("Helvetica");
        textBoxCustomization.textFontSize(15);

        ThreeDSecureV2ToolbarCustomization toolbarCustomization = new ThreeDSecureV2ToolbarCustomization();
        toolbarCustomization.backgroundColor("#FFFFFF");
        toolbarCustomization.buttonText("Button");
        toolbarCustomization.headerText("Header");
        toolbarCustomization.textColor("#121212");
        toolbarCustomization.textFontName("Helvetica");
        toolbarCustomization.textFontSize(15);

        ThreeDSecureV2UiCustomization customization = new ThreeDSecureV2UiCustomization();
        customization.buttonCustomization(buttonCustomization, ThreeDSecureV2UiCustomization.BUTTON_TYPE_CONTINUE);
        customization.labelCustomization(labelCustomization);
        customization.textBoxCustomization(textBoxCustomization);
        customization.toolbarCustomization(toolbarCustomization);

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV2UiCustomization actual = (ThreeDSecureV2UiCustomization) ThreeDSecureV2UiCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("#FF0000", actual.getButtonCustomization().getBackgroundColor());
        assertEquals(5, actual.getButtonCustomization().getCornerRadius());
        assertEquals("#000000", actual.getButtonCustomization().getTextColor());
        assertEquals("Comic Sans", actual.getButtonCustomization().getTextFontName());
        assertEquals(20, actual.getButtonCustomization().getTextFontSize());

        assertEquals("#FF0000", actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE).getBackgroundColor());
        assertEquals(5, actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE).getCornerRadius());
        assertEquals("#000000", actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE).getTextColor());
        assertEquals("Comic Sans", actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE).getTextFontName());
        assertEquals(20, actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE).getTextFontSize());

        assertEquals("#FFFFFF", actual.getLabelCustomization().getHeadingTextColor());
        assertEquals("Times New Roman", actual.getLabelCustomization().getHeadingTextFontName());
        assertEquals(30, actual.getLabelCustomization().getHeadingTextFontSize());
        assertEquals("#121212", actual.getLabelCustomization().getTextColor());
        assertEquals("Helvetica", actual.getLabelCustomization().getTextFontName());
        assertEquals(15, actual.getLabelCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getCardinalUiCustomization().getLabelCustomization().getHeadingTextColor());
        assertEquals("Times New Roman", actual.getCardinalUiCustomization().getLabelCustomization().getHeadingTextFontName());
        assertEquals(30, actual.getCardinalUiCustomization().getLabelCustomization().getHeadingTextFontSize());
        assertEquals("#121212", actual.getCardinalUiCustomization().getLabelCustomization().getTextColor());
        assertEquals("Helvetica", actual.getCardinalUiCustomization().getLabelCustomization().getTextFontName());
        assertEquals(15, actual.getCardinalUiCustomization().getLabelCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getTextBoxCustomization().getBorderColor());
        assertEquals(10, actual.getTextBoxCustomization().getBorderWidth());
        assertEquals(5, actual.getTextBoxCustomization().getCornerRadius());
        assertEquals("#121212", actual.getTextBoxCustomization().getTextColor());
        assertEquals("Helvetica", actual.getTextBoxCustomization().getTextFontName());
        assertEquals(15, actual.getTextBoxCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getCardinalUiCustomization().getTextBoxCustomization().getBorderColor());
        assertEquals(10, actual.getCardinalUiCustomization().getTextBoxCustomization().getBorderWidth());
        assertEquals(5, actual.getCardinalUiCustomization().getTextBoxCustomization().getCornerRadius());
        assertEquals("#121212", actual.getCardinalUiCustomization().getTextBoxCustomization().getTextColor());
        assertEquals("Helvetica", actual.getCardinalUiCustomization().getTextBoxCustomization().getTextFontName());
        assertEquals(15, actual.getCardinalUiCustomization().getTextBoxCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getToolbarCustomization().getBackgroundColor());
        assertEquals("Button", actual.getToolbarCustomization().getButtonText());
        assertEquals("Header", actual.getToolbarCustomization().getHeaderText());
        assertEquals("#121212", actual.getToolbarCustomization().getTextColor());
        assertEquals("Helvetica", actual.getToolbarCustomization().getTextFontName());
        assertEquals(15, actual.getToolbarCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getCardinalUiCustomization().getToolbarCustomization().getBackgroundColor());
        assertEquals("Button", actual.getCardinalUiCustomization().getToolbarCustomization().getButtonText());
        assertEquals("Header", actual.getCardinalUiCustomization().getToolbarCustomization().getHeaderText());
        assertEquals("#121212", actual.getCardinalUiCustomization().getToolbarCustomization().getTextColor());
        assertEquals("Helvetica", actual.getCardinalUiCustomization().getToolbarCustomization().getTextFontName());
        assertEquals(15, actual.getCardinalUiCustomization().getToolbarCustomization().getTextFontSize());
    }
}
