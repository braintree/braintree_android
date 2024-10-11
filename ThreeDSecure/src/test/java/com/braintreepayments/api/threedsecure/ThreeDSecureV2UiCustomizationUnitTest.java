package com.braintreepayments.api.threedsecure;

import android.os.Parcel;

import com.braintreepayments.api.threedsecure.ThreeDSecureV2ButtonCustomization;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2LabelCustomization;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2TextBoxCustomization;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2ToolbarCustomization;
import com.braintreepayments.api.threedsecure.ThreeDSecureV2UiCustomization;
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
        sut.setLabelCustomization(new ThreeDSecureV2LabelCustomization());
        sut.setButtonCustomization(new ThreeDSecureV2ButtonCustomization());
        sut.setButtonType(ThreeDSecureV2ButtonType.BUTTON_TYPE_NEXT);
        sut.setTextBoxCustomization(new ThreeDSecureV2TextBoxCustomization());
        sut.setToolbarCustomization(new ThreeDSecureV2ToolbarCustomization());

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
        ThreeDSecureV2ButtonCustomization buttonCustomization =
                new ThreeDSecureV2ButtonCustomization();
        buttonCustomization.setBackgroundColor("#FF0000");
        buttonCustomization.setCornerRadius(5);
        buttonCustomization.setTextColor("#000000");
        buttonCustomization.setTextFontName("Comic Sans");
        buttonCustomization.setTextFontSize(20);

        ThreeDSecureV2LabelCustomization labelCustomization =
                new ThreeDSecureV2LabelCustomization();
        labelCustomization.setHeadingTextColor("#FFFFFF");
        labelCustomization.setHeadingTextFontName("Times New Roman");
        labelCustomization.setHeadingTextFontSize(30);
        labelCustomization.setTextColor("#121212");
        labelCustomization.setTextFontName("Helvetica");
        labelCustomization.setTextFontSize(15);

        ThreeDSecureV2TextBoxCustomization textBoxCustomization =
                new ThreeDSecureV2TextBoxCustomization();
        textBoxCustomization.setBorderColor("#FFFFFF");
        textBoxCustomization.setBorderWidth(10);
        textBoxCustomization.setCornerRadius(5);
        textBoxCustomization.setTextColor("#121212");
        textBoxCustomization.setTextFontName("Helvetica");
        textBoxCustomization.setTextFontSize(15);

        ThreeDSecureV2ToolbarCustomization toolbarCustomization =
                new ThreeDSecureV2ToolbarCustomization();
        toolbarCustomization.setBackgroundColor("#FFFFFF");
        toolbarCustomization.setButtonText("Button");
        toolbarCustomization.setHeaderText("Header");
        toolbarCustomization.setTextColor("#121212");
        toolbarCustomization.setTextFontName("Helvetica");
        toolbarCustomization.setTextFontSize(15);

        ThreeDSecureV2UiCustomization customization = new ThreeDSecureV2UiCustomization();
        customization.setButtonCustomization(buttonCustomization);
        customization.setButtonType(ThreeDSecureV2ButtonType.BUTTON_TYPE_CONTINUE);
        customization.setLabelCustomization(labelCustomization);
        customization.setTextBoxCustomization(textBoxCustomization);
        customization.setToolbarCustomization(toolbarCustomization);

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV2UiCustomization actual =
                ThreeDSecureV2UiCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("#FF0000", actual.getButtonCustomization().getBackgroundColor());
        assertEquals(5, actual.getButtonCustomization().getCornerRadius());
        assertEquals("#000000", actual.getButtonCustomization().getTextColor());
        assertEquals("Comic Sans", actual.getButtonCustomization().getTextFontName());
        assertEquals(20, actual.getButtonCustomization().getTextFontSize());

        assertEquals("#FF0000",
                actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE)
                        .getBackgroundColor());
        assertEquals(5,
                actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE)
                        .getCornerRadius());
        assertEquals("#000000",
                actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE)
                        .getTextColor());
        assertEquals("Comic Sans",
                actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE)
                        .getTextFontName());
        assertEquals(20,
                actual.getCardinalUiCustomization().getButtonCustomization(ButtonType.CONTINUE)
                        .getTextFontSize());

        assertEquals("#FFFFFF", actual.getLabelCustomization().getHeadingTextColor());
        assertEquals("Times New Roman", actual.getLabelCustomization().getHeadingTextFontName());
        assertEquals(30, actual.getLabelCustomization().getHeadingTextFontSize());
        assertEquals("#121212", actual.getLabelCustomization().getTextColor());
        assertEquals("Helvetica", actual.getLabelCustomization().getTextFontName());
        assertEquals(15, actual.getLabelCustomization().getTextFontSize());

        assertEquals("#FFFFFF",
                actual.getCardinalUiCustomization().getLabelCustomization().getHeadingTextColor());
        assertEquals("Times New Roman", actual.getCardinalUiCustomization().getLabelCustomization()
                .getHeadingTextFontName());
        assertEquals(30, actual.getCardinalUiCustomization().getLabelCustomization()
                .getHeadingTextFontSize());
        assertEquals("#121212",
                actual.getCardinalUiCustomization().getLabelCustomization().getTextColor());
        assertEquals("Helvetica",
                actual.getCardinalUiCustomization().getLabelCustomization().getTextFontName());
        assertEquals(15,
                actual.getCardinalUiCustomization().getLabelCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getTextBoxCustomization().getBorderColor());
        assertEquals(10, actual.getTextBoxCustomization().getBorderWidth());
        assertEquals(5, actual.getTextBoxCustomization().getCornerRadius());
        assertEquals("#121212", actual.getTextBoxCustomization().getTextColor());
        assertEquals("Helvetica", actual.getTextBoxCustomization().getTextFontName());
        assertEquals(15, actual.getTextBoxCustomization().getTextFontSize());

        assertEquals("#FFFFFF",
                actual.getCardinalUiCustomization().getTextBoxCustomization().getBorderColor());
        assertEquals(10,
                actual.getCardinalUiCustomization().getTextBoxCustomization().getBorderWidth());
        assertEquals(5,
                actual.getCardinalUiCustomization().getTextBoxCustomization().getCornerRadius());
        assertEquals("#121212",
                actual.getCardinalUiCustomization().getTextBoxCustomization().getTextColor());
        assertEquals("Helvetica",
                actual.getCardinalUiCustomization().getTextBoxCustomization().getTextFontName());
        assertEquals(15,
                actual.getCardinalUiCustomization().getTextBoxCustomization().getTextFontSize());

        assertEquals("#FFFFFF", actual.getToolbarCustomization().getBackgroundColor());
        assertEquals("Button", actual.getToolbarCustomization().getButtonText());
        assertEquals("Header", actual.getToolbarCustomization().getHeaderText());
        assertEquals("#121212", actual.getToolbarCustomization().getTextColor());
        assertEquals("Helvetica", actual.getToolbarCustomization().getTextFontName());
        assertEquals(15, actual.getToolbarCustomization().getTextFontSize());

        assertEquals("#FFFFFF",
                actual.getCardinalUiCustomization().getToolbarCustomization().getBackgroundColor());
        assertEquals("Button",
                actual.getCardinalUiCustomization().getToolbarCustomization().getButtonText());
        assertEquals("Header",
                actual.getCardinalUiCustomization().getToolbarCustomization().getHeaderText());
        assertEquals("#121212",
                actual.getCardinalUiCustomization().getToolbarCustomization().getTextColor());
        assertEquals("Helvetica",
                actual.getCardinalUiCustomization().getToolbarCustomization().getTextFontName());
        assertEquals(15,
                actual.getCardinalUiCustomization().getToolbarCustomization().getTextFontSize());
    }
}
