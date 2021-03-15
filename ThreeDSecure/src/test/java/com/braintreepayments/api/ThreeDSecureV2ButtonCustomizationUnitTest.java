package com.braintreepayments.api;

import android.os.Parcel;

import com.cardinalcommerce.shared.userinterfaces.ButtonCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2ButtonCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2ButtonCustomization sut = new ThreeDSecureV2ButtonCustomization();
        sut.backgroundColor("#00FF00");
        sut.cornerRadius(5);
        sut.textColor("#ff0000");
        sut.textFontSize(11);
        sut.textFontName("Times New Roman");

        ButtonCustomization cardinalButtonCustomization = sut.getCardinalButtonCustomization();
        assertEquals("#00FF00", cardinalButtonCustomization.getBackgroundColor());
        assertEquals(5, cardinalButtonCustomization.getCornerRadius());
        assertEquals("#ff0000", cardinalButtonCustomization.getTextColor());
        assertEquals(11, cardinalButtonCustomization.getTextFontSize());
        assertEquals("Times New Roman", cardinalButtonCustomization.getTextFontName());
    }

    @Test
    public void writeToParcel() {
        ThreeDSecureV2ButtonCustomization customization = new ThreeDSecureV2ButtonCustomization();
        customization.backgroundColor("#FFFFFF");
        customization.cornerRadius(5);
        customization.textColor("#121212");
        customization.textFontName("Helvetica");
        customization.textFontSize(15);

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV2ButtonCustomization actual = ThreeDSecureV2ButtonCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("#FFFFFF", actual.getBackgroundColor());
        assertEquals(5, actual.getCornerRadius());
        assertEquals("#121212", actual.getTextColor());
        assertEquals("Helvetica", actual.getTextFontName());
        assertEquals(15, actual.getTextFontSize());
    }
}