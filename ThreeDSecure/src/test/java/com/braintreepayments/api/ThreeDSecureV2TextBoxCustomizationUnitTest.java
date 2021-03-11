package com.braintreepayments.api;

import com.cardinalcommerce.shared.userinterfaces.Customization;
import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2TextBoxCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2TextBoxCustomization sut = (ThreeDSecureV2TextBoxCustomization) new ThreeDSecureV2TextBoxCustomization()
                .borderColor("#FFA500")
                .borderWidth(1)
                .cornerRadius(7)
                .textColor("#0000ff")
                .textFontSize(5)
                .textFontName("Arial");

        TextBoxCustomization cardinalTextBoxCustomization = sut.getCardinalTextBoxCustomization();
        assertEquals("#FFA500", cardinalTextBoxCustomization.getBorderColor());
        assertEquals(1, cardinalTextBoxCustomization.getBorderWidth());
        assertEquals(7, cardinalTextBoxCustomization.getCornerRadius());

        Customization cardinalCustomization = sut.getCardinalCustomization();
        assertEquals("#0000ff", cardinalCustomization.getTextColor());
        assertEquals(5, cardinalCustomization.getTextFontSize());
        assertEquals("Arial", cardinalCustomization.getTextFontName());
    }
}
