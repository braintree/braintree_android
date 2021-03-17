package com.braintreepayments.api;

import android.os.Parcel;

import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2TextBoxCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2TextBoxCustomization sut = new ThreeDSecureV2TextBoxCustomization();
        sut.setBorderColor("#FFA500");
        sut.setBorderWidth(1);
        sut.setCornerRadius(7);
        sut.setTextColor("#0000ff");
        sut.setTextFontSize(5);
        sut.setTextFontName("Arial");

        TextBoxCustomization cardinalTextBoxCustomization = sut.getCardinalTextBoxCustomization();
        assertEquals("#FFA500", cardinalTextBoxCustomization.getBorderColor());
        assertEquals(1, cardinalTextBoxCustomization.getBorderWidth());
        assertEquals(7, cardinalTextBoxCustomization.getCornerRadius());
        assertEquals("#0000ff", cardinalTextBoxCustomization.getTextColor());
        assertEquals(5, cardinalTextBoxCustomization.getTextFontSize());
        assertEquals("Arial", cardinalTextBoxCustomization.getTextFontName());
    }

    @Test
    public void writeToParcel() {
        ThreeDSecureV2TextBoxCustomization customization = new ThreeDSecureV2TextBoxCustomization();
        customization.setBorderColor("#FFFFFF");
        customization.setBorderWidth(10);
        customization.setCornerRadius(5);
        customization.setTextColor("#121212");
        customization.setTextFontName("Helvetica");
        customization.setTextFontSize(15);

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV2TextBoxCustomization actual = (ThreeDSecureV2TextBoxCustomization) ThreeDSecureV2TextBoxCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("#FFFFFF", actual.getBorderColor());
        assertEquals(10, actual.getBorderWidth());
        assertEquals(5, actual.getCornerRadius());
        assertEquals("#121212", actual.getTextColor());
        assertEquals("Helvetica", actual.getTextFontName());
        assertEquals(15, actual.getTextFontSize());
    }
}
