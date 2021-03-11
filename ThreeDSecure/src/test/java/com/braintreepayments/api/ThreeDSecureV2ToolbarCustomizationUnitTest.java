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

    @Test
    public void writeToParcel() {
        ThreeDSecureV2ToolbarCustomization customization = (ThreeDSecureV2ToolbarCustomization) new ThreeDSecureV2ToolbarCustomization()
                .backgroundColor("#FFFFFF")
                .buttonText("Button")
                .headerText("Header")
                .textColor("#121212")
                .textFontName("Helvetica")
                .textFontSize(15);

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
