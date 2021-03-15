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
        sut.headingTextColor("#00FF00");
        sut.headingTextFontName("Comic Sans");
        sut.headingTextFontSize(12);
        sut.textColor("#ff0000");
        sut.textFontSize(19);
        sut.textFontName("Arial");

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
        customization.headingTextColor("#FFFFFF");
        customization.headingTextFontName("Times New Roman");
        customization.headingTextFontSize(30);
        customization.textColor("#121212");
        customization.textFontName("Helvetica");
        customization.textFontSize(15);

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