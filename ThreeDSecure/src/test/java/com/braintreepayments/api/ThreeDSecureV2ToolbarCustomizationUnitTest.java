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
        sut.setBackgroundColor("#0000ff");
        sut.setHeaderText("Header Text");
        sut.setButtonText("Button");
        sut.setTextColor("#FF0000");
        sut.setTextFontSize(12);
        sut.setTextFontName("Helvetica");

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
        customization.setBackgroundColor("#FFFFFF");
        customization.setButtonText("Button");
        customization.setHeaderText("Header");
        customization.setTextColor("#121212");
        customization.setTextFontName("Helvetica");
        customization.setTextFontSize(15);

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
