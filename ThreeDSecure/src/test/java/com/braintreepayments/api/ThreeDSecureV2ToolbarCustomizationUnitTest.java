package com.braintreepayments.api;


import com.cardinalcommerce.shared.userinterfaces.Customization;
import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2ToolbarCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2ToolbarCustomization sut = (ThreeDSecureV2ToolbarCustomization) new ThreeDSecureV2ToolbarCustomization()
                .backgroundColor("#0000ff")
                .headerText("Header Text")
                .buttonText("Button")
                .textColor("#FF0000")
                .textFontSize(12)
                .textFontName("Helvetica");

        ToolbarCustomization cardinalToolbarCustomization = sut.getCardinalToolbarCustomization();
        assertEquals("#0000ff", cardinalToolbarCustomization.getBackgroundColor());
        assertEquals("Header Text", cardinalToolbarCustomization.getHeaderText());
        assertEquals("Button", cardinalToolbarCustomization.getButtonText());
        assertEquals("#FF0000", cardinalToolbarCustomization.getTextColor());
        assertEquals(12, cardinalToolbarCustomization.getTextFontSize());
        assertEquals("Helvetica", cardinalToolbarCustomization.getTextFontName());
    }
}
