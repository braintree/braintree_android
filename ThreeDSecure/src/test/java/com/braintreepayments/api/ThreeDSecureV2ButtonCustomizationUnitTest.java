package com.braintreepayments.api;

import com.cardinalcommerce.shared.userinterfaces.ButtonCustomization;
import com.cardinalcommerce.shared.userinterfaces.Customization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2ButtonCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2ButtonCustomization sut = (ThreeDSecureV2ButtonCustomization) new ThreeDSecureV2ButtonCustomization()
                .backgroundColor("#00FF00")
                .cornerRadius(5)
                .textColor("#ff0000")
                .textFontSize(11)
                .textFontName("Times New Roman");

        ButtonCustomization cardinalButtonCustomization = sut.getCardinalButtonCustomization();
        assertEquals("#00FF00", cardinalButtonCustomization.getBackgroundColor());
        assertEquals(5, cardinalButtonCustomization.getCornerRadius());
        assertEquals("#ff0000", cardinalButtonCustomization.getTextColor());
        assertEquals(11, cardinalButtonCustomization.getTextFontSize());
        assertEquals("Times New Roman", cardinalButtonCustomization.getTextFontName());
    }

}