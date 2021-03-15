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
        sut.borderColor("#FFA500");
        sut.borderWidth(1);
        sut.cornerRadius(7);
        sut.textColor("#0000ff");
        sut.textFontSize(5);
        sut.textFontName("Arial");

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
        customization.borderColor("#FFFFFF");
        customization.borderWidth(10);
        customization.cornerRadius(5);
        customization.textColor("#121212");
        customization.textFontName("Helvetica");
        customization.textFontSize(15);

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
