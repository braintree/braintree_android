package com.braintreepayments.api;

import com.cardinalcommerce.shared.userinterfaces.Customization;
import com.cardinalcommerce.shared.userinterfaces.LabelCustomization;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2LabelCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2LabelCustomization sut = (ThreeDSecureV2LabelCustomization) new ThreeDSecureV2LabelCustomization()
                .headingTextColor("#00FF00")
                .headingTextFontName("Comic Sans")
                .headingTextFontSize(12)
                .textColor("#ff0000")
                .textFontSize(19)
                .textFontName("Arial");

        LabelCustomization cardinalLabelCustomization = sut.getCardinalLabelCustomization();
        assertEquals("#00FF00", cardinalLabelCustomization.getHeadingTextColor());
        assertEquals("Comic Sans", cardinalLabelCustomization.getHeadingTextFontName());
        assertEquals(12, cardinalLabelCustomization.getHeadingTextFontSize());

        Customization cardinalCustomization = sut.getCardinalCustomization();
        assertEquals("#ff0000", cardinalCustomization.getTextColor());
        assertEquals(19, cardinalCustomization.getTextFontSize());
        assertEquals("Arial", cardinalCustomization.getTextFontName());
    }
}