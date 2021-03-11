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

    @Test
    public void writeToParcel() {
        ThreeDSecureV2ButtonCustomization customization = (ThreeDSecureV2ButtonCustomization) new ThreeDSecureV2ButtonCustomization()
                .backgroundColor("#FFFFFF")
                .cornerRadius(5)
                .textColor("#121212")
                .textFontName("Helvetica")
                .textFontSize(15);

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV2ButtonCustomization actual = (ThreeDSecureV2ButtonCustomization) ThreeDSecureV2ButtonCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("#FFFFFF", actual.getBackgroundColor());
        assertEquals(5, actual.getCornerRadius());
        assertEquals("#121212", actual.getTextColor());
        assertEquals("Helvetica", actual.getTextFontName());
        assertEquals(15, actual.getTextFontSize());
    }
}