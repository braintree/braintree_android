package com.braintreepayments.api.threedsecure

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureV2ToolbarCustomizationUnitTest {

    @Test
    fun `sets all cardinal class properties`() {
        val sut = ThreeDSecureV2ToolbarCustomization(
            backgroundColor = "#0000ff",
            headerText = "Header Text",
            buttonText = "Button",
            textColor = "#FF0000",
            textFontSize = 12,
            textFontName = "Helvetica"
        )

        val cardinalToolbarCustomization = sut.cardinalToolbarCustomization
        assertEquals("#0000ff", cardinalToolbarCustomization.backgroundColor)
        assertEquals("Header Text", cardinalToolbarCustomization.headerText)
        assertEquals("Button", cardinalToolbarCustomization.buttonText)
        assertEquals("#FF0000", cardinalToolbarCustomization.textColor)
        assertEquals(12, cardinalToolbarCustomization.textFontSize)
        assertEquals("Helvetica", cardinalToolbarCustomization.textFontName)
    }

    @Test
    fun `write to parcel`() {
        val sut = ThreeDSecureV2ToolbarCustomization(
            backgroundColor = "#FFFFFF",
            buttonText = "Button",
            headerText = "Header",
            textColor = "#121212",
            textFontName = "Helvetica",
            textFontSize = 15
        )

        val parcel = Parcel.obtain().apply {
            sut.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val parceled = parcelableCreator<ThreeDSecureV2ToolbarCustomization>().createFromParcel(parcel)

        assertEquals("#FFFFFF", parceled.backgroundColor)
        assertEquals("Button", parceled.buttonText)
        assertEquals("Header", parceled.headerText)
        assertEquals("#121212", parceled.textColor)
        assertEquals("Helvetica", parceled.textFontName)
        assertEquals(15, parceled.textFontSize)
    }
}
