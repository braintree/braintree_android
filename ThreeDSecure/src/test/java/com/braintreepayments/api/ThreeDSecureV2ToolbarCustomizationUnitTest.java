package com.braintreepayments.api;


import android.os.Parcel;

import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2ToolbarCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2ToolbarCustomization sut = new ThreeDSecureV2ToolbarCustomization();
        sut.backgroundColor("#0000ff");
        sut.headerText("Header Text");
        sut.buttonText("Button");
        sut.textColor("#FF0000");
        sut.textFontSize(12);
        sut.textFontName("Helvetica");

        ToolbarCustomization cardinalToolbarCustomization = sut.getCardinalToolbarCustomization();
        assertEquals("#0000ff", cardinalToolbarCustomization.getBackgroundColor());
        assertEquals("Header Text", cardinalToolbarCustomization.getHeaderText());
        assertEquals("Button", cardinalToolbarCustomization.getButtonText());
        assertEquals("#FF0000", cardinalToolbarCustomization.getTextColor());
        assertEquals(12, cardinalToolbarCustomization.getTextFontSize());
        assertEquals("Helvetica", cardinalToolbarCustomization.getTextFontName());
    }

    @Test
    public void writeToParcel() {
        ThreeDSecureV2ToolbarCustomization customization = new ThreeDSecureV2ToolbarCustomization();
        customization.backgroundColor("#FFFFFF");
        customization.buttonText("Button");
        customization.headerText("Header");
        customization.textColor("#121212");
        customization.textFontName("Helvetica");
        customization.textFontSize(15);

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV2ToolbarCustomization actual = (ThreeDSecureV2ToolbarCustomization) ThreeDSecureV2ToolbarCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("#FFFFFF", actual.getBackgroundColor());
        assertEquals("Button", actual.getButtonText());
        assertEquals("Header", actual.getHeaderText());
        assertEquals("#121212", actual.getTextColor());
        assertEquals("Helvetica", actual.getTextFontName());
        assertEquals(15, actual.getTextFontSize());
    }
}
