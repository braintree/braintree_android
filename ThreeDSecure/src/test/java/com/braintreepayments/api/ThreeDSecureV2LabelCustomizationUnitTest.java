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
        assertEquals("#ff0000", cardinalLabelCustomization.getTextColor());
        assertEquals(19, cardinalLabelCustomization.getTextFontSize());
        assertEquals("Arial", cardinalLabelCustomization.getTextFontName());
    }

    @Test
    public void writeToParcel() {
        ThreeDSecureV2LabelCustomization customization = (ThreeDSecureV2LabelCustomization) new ThreeDSecureV2LabelCustomization()
                .headingTextColor("#FFFFFF")
                .headingTextFontName("Times New Roman")
                .headingTextFontSize(30)
                .textColor("#121212")
                .textFontName("Helvetica")
                .textFontSize(15);

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