package com.braintreepayments.api.threedsecure

import android.os.Parcel
import junit.framework.TestCase.assertEquals
import kotlinx.parcelize.parcelableCreator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureV2LabelCustomizationUnitTest {

    @Test
    fun `sets all cardinalClass properties correctly`() {
        val sut = ThreeDSecureV2LabelCustomization(
            headingTextColor = "#00FF00",
            headingTextFontName = "Comic Sans",
            headingTextFontSize = 12,
            textColor = "#ff0000",
            textFontName = "Arial",
            textFontSize = 19
        )

        val cardinalLabelCustomization = sut.cardinalLabelCustomization

        assertEquals("#00FF00", cardinalLabelCustomization.headingTextColor)
        assertEquals("Comic Sans", cardinalLabelCustomization.headingTextFontName)
        assertEquals(12, cardinalLabelCustomization.headingTextFontSize)
        assertEquals("#ff0000", cardinalLabelCustomization.textColor)
        assertEquals(19, cardinalLabelCustomization.textFontSize)
        assertEquals("Arial", cardinalLabelCustomization.textFontName)
    }

    @Test
    fun `creates ThreeDSecureV2LabelCustomization and parcels it correctly`() {
        val customization = ThreeDSecureV2LabelCustomization(
            headingTextColor = "#FFFFFF",
            headingTextFontName = "Times New Roman",
            headingTextFontSize = 30,
            textColor = "#121212",
            textFontName = "Helvetica",
            textFontSize = 15
        )

        val parcel = Parcel.obtain().apply {
            customization.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val actual = parcelableCreator<ThreeDSecureV2LabelCustomization>().createFromParcel(parcel)
        assertEquals("#FFFFFF", actual.headingTextColor)
        assertEquals("Times New Roman", actual.headingTextFontName)
        assertEquals(30, actual.headingTextFontSize)
        assertEquals("#121212", actual.textColor)
        assertEquals(15, actual.textFontSize)
        assertEquals("Helvetica", actual.textFontName)
    }
}
