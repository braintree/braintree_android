package com.braintreepayments.api;

import android.os.Parcel;

import com.cardinalcommerce.shared.userinterfaces.LabelCustomization;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureV2LabelCustomizationUnitTest {

    @Test
    public void setsAllCardinalClassProperties() {
        ThreeDSecureV2LabelCustomization sut = new ThreeDSecureV2LabelCustomization();
        sut.setHeadingTextColor("#00FF00");
        sut.setHeadingTextFontName("Comic Sans");
        sut.setHeadingTextFontSize(12);
        sut.setTextColor("#ff0000");
        sut.setTextFontSize(19);
        sut.setTextFontName("Arial");

        LabelCustomization cardinalLabelCustomization = sut.getCardinalLabelCustomization();
        assertEquals("#00FF00", cardinalLabelCustomization.getHeadingTextColor());
        assertEquals("Comic Sans", cardinalLabelCustomization.getHeadingTextFontName());
        assertEquals(12, cardinalLabelCustomization.getHeadingTextFontSize());
        assertEquals("#ff0000", cardinalLabelCustomization.getTextColor());
        assertEquals(19, cardinalLabelCustomization.getTextFontSize());
        assertEquals("Arial", cardinalLabelCustomization.getTextFontName());
    }

    @Test
    public void writeToParcel() {
        ThreeDSecureV2LabelCustomization customization = new ThreeDSecureV2LabelCustomization();
        customization.setHeadingTextColor("#FFFFFF");
        customization.setHeadingTextFontName("Times New Roman");
        customization.setHeadingTextFontSize(30);
        customization.setTextColor("#121212");
        customization.setTextFontName("Helvetica");
        customization.setTextFontSize(15);

        Parcel parcel = Parcel.obtain();
        customization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureV2LabelCustomization actual = (ThreeDSecureV2LabelCustomization) ThreeDSecureV2LabelCustomization.CREATOR.createFromParcel(parcel);

        assertEquals("#FFFFFF", actual.getHeadingTextColor());
        assertEquals("Times New Roman", actual.getHeadingTextFontName());
        assertEquals(30, actual.getHeadingTextFontSize());
        assertEquals("#121212", actual.getTextColor());
        assertEquals("Helvetica", actual.getTextFontName());
        assertEquals(15, actual.getTextFontSize());
    }
}