package com.braintreepayments.api.threedsecure

import android.os.Parcel
import junit.framework.TestCase.assertEquals
import kotlinx.parcelize.parcelableCreator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureV2ButtonCustomizationUnitTest {

    @Test
    fun `sets all cardinalClass properties correctly`() {
        val sut = ThreeDSecureV2ButtonCustomization(
            backgroundColor = "#00FF00",
            cornerRadius = 5,
            textColor = "#ff0000",
            textFontSize = 11,
            textFontName = "Times New Roman"
        )

        val cardinalButtonCustomization = sut.cardinalButtonCustomization

        assertEquals("#00FF00", cardinalButtonCustomization.backgroundColor)
        assertEquals(5, cardinalButtonCustomization.cornerRadius)
        assertEquals("#ff0000", cardinalButtonCustomization.textColor)
        assertEquals(11, cardinalButtonCustomization.textFontSize)
        assertEquals("Times New Roman", cardinalButtonCustomization.textFontName)
    }

    @Test
    fun `creates ThreeDSecureV2ButtonCustomization and parcels it correctly`() {
        val customization = ThreeDSecureV2ButtonCustomization(
            backgroundColor = "#FFFFFF",
            cornerRadius = 5,
            textColor = "#121212",
            textFontName = "Helvetica",
            textFontSize = 15
        )

        val parcel = Parcel.obtain().apply {
            customization.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val actual = parcelableCreator<ThreeDSecureV2ButtonCustomization>()
                        .createFromParcel(parcel)
        assertEquals("#FFFFFF", actual.backgroundColor)
        assertEquals(5, actual.cornerRadius)
        assertEquals("#121212", actual.textColor)
        assertEquals("Helvetica", actual.textFontName)
        assertEquals(15, actual.textFontSize)
    }
}
