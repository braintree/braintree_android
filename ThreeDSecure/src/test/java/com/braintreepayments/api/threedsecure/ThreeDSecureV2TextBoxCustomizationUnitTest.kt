package com.braintreepayments.api.threedsecure

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureV2TextBoxCustomizationUnitTest {

    @Test
    fun `sets all cardinal class properties`() {
        val sut = ThreeDSecureV2TextBoxCustomization(
            borderColor = "#FFA500",
            borderWidth = 1,
            cornerRadius = 7,
            textColor = "#0000ff",
            textFontSize = 5,
            textFontName = "Arial"
        )

        val cardinalTextBoxCustomization = sut.cardinalTextBoxCustomization
        assertEquals("#FFA500", cardinalTextBoxCustomization.borderColor)
        assertEquals(1, cardinalTextBoxCustomization.borderWidth)
        assertEquals(7, cardinalTextBoxCustomization.cornerRadius)
        assertEquals("#0000ff", cardinalTextBoxCustomization.textColor)
        assertEquals(5, cardinalTextBoxCustomization.textFontSize)
        assertEquals("Arial", cardinalTextBoxCustomization.textFontName)
    }

    @Test
    fun `write to parcel`() {
        val sut = ThreeDSecureV2TextBoxCustomization(
            borderColor = "#FFFFFF",
            borderWidth = 10,
            cornerRadius = 5,
            textColor = "#121212",
            textFontName = "Helvetica",
            textFontSize = 15,
        )
        val parcel = Parcel.obtain().apply {
            sut.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val parceled = parcelableCreator<ThreeDSecureV2TextBoxCustomization>().createFromParcel(parcel)

        assertEquals("#FFFFFF", parceled.borderColor)
        assertEquals(10, parceled.borderWidth)
        assertEquals(5, parceled.cornerRadius)
        assertEquals("#121212", parceled.textColor)
        assertEquals("Helvetica", parceled.textFontName)
        assertEquals(15, parceled.textFontSize)
    }
}
