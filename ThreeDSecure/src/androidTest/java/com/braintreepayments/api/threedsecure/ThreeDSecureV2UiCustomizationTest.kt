package com.braintreepayments.api.threedsecure

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.parcelize.parcelableCreator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ThreeDSecureV2UiCustomizationTest {

    @Test
    fun parcels_withAllNestedCustomizations() {
        val original = ThreeDSecureV2UiCustomization(
            buttonCustomization = ThreeDSecureV2ButtonCustomization(
                textFontName = "Roboto",
                textColor = "#000000",
                textFontSize = 16,
                backgroundColor = "#D3D3D3",
                cornerRadius = 8
            ),
            buttonType = ThreeDSecureV2ButtonType.BUTTON_TYPE_VERIFY,
            labelCustomization = ThreeDSecureV2LabelCustomization(
                textFontName = "Roboto",
                textColor = "#333333",
                textFontSize = 14,
                headingTextColor = "#0082CB",
                headingTextFontName = "Roboto-Bold",
                headingTextFontSize = 18
            ),
            textBoxCustomization = ThreeDSecureV2TextBoxCustomization(
                textFontName = "Roboto",
                textColor = "#111111",
                textFontSize = 14,
                borderWidth = 2,
                borderColor = "#0082CB",
                cornerRadius = 4
            ),
            toolbarCustomization = ThreeDSecureV2ToolbarCustomization(
                textFontName = "Roboto",
                textColor = "#222222",
                textFontSize = 18,
                backgroundColor = "#FF5A5F",
                headerText = "Braintree 3DS Checkout",
                buttonText = "Close"
            )
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureV2UiCustomization>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals("#D3D3D3", restored.buttonCustomization?.backgroundColor)
        assertEquals("#000000", restored.buttonCustomization?.textColor)
        assertEquals("Roboto", restored.buttonCustomization?.textFontName)
        assertEquals(16, restored.buttonCustomization?.textFontSize)
        assertEquals(8, restored.buttonCustomization?.cornerRadius)
        assertEquals(ThreeDSecureV2ButtonType.BUTTON_TYPE_VERIFY, restored.buttonType)

        assertEquals("#0082CB", restored.labelCustomization?.headingTextColor)
        assertEquals("Roboto-Bold", restored.labelCustomization?.headingTextFontName)
        assertEquals(18, restored.labelCustomization?.headingTextFontSize)

        assertEquals(2, restored.textBoxCustomization?.borderWidth)
        assertEquals("#0082CB", restored.textBoxCustomization?.borderColor)
        assertEquals(4, restored.textBoxCustomization?.cornerRadius)

        assertEquals("Braintree 3DS Checkout", restored.toolbarCustomization?.headerText)
        assertEquals("Close", restored.toolbarCustomization?.buttonText)
        assertEquals("#FF5A5F", restored.toolbarCustomization?.backgroundColor)
    }

    @Test
    fun parcels_withDefaultValues() {
        val original = ThreeDSecureV2UiCustomization()

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureV2UiCustomization>().createFromParcel(parcel)
        parcel.recycle()

        assertNull(restored.buttonCustomization)
        assertNull(restored.buttonType)
        assertNull(restored.labelCustomization)
        assertNull(restored.textBoxCustomization)
        assertNull(restored.toolbarCustomization)
    }

    @Test
    fun cardinalUiCustomization_bridgesAllProperties() {
        val uiCustomization = ThreeDSecureV2UiCustomization(
            buttonCustomization = ThreeDSecureV2ButtonCustomization(
                textFontName = "Arial",
                textColor = "#FF0000",
                textFontSize = 20,
                backgroundColor = "#00FF00",
                cornerRadius = 12
            ),
            buttonType = ThreeDSecureV2ButtonType.BUTTON_TYPE_VERIFY,
            labelCustomization = ThreeDSecureV2LabelCustomization(
                headingTextColor = "#0082CB",
                headingTextFontName = "Arial-Bold",
                headingTextFontSize = 22,
                textFontSize = 14
            ),
            textBoxCustomization = ThreeDSecureV2TextBoxCustomization(
                borderColor = "#0082CB",
                borderWidth = 3,
                cornerRadius = 6
            ),
            toolbarCustomization = ThreeDSecureV2ToolbarCustomization(
                headerText = "Checkout",
                buttonText = "Done",
                backgroundColor = "#FFFFFF"
            )
        )

        val cardinal = uiCustomization.cardinalUiCustomization
        assertNotNull(cardinal)

        val btn = checkNotNull(uiCustomization.buttonCustomization).cardinalButtonCustomization
        assertEquals("Arial", btn.textFontName)
        assertEquals("#FF0000", btn.textColor)
        assertEquals(20, btn.textFontSize)
        assertEquals("#00FF00", btn.backgroundColor)
        assertEquals(12, btn.cornerRadius)

        val lbl = checkNotNull(uiCustomization.labelCustomization).cardinalLabelCustomization
        assertEquals("#0082CB", lbl.headingTextColor)
        assertEquals(22, lbl.headingTextFontSize)

        val txt = checkNotNull(uiCustomization.textBoxCustomization).cardinalTextBoxCustomization
        assertEquals("#0082CB", txt.borderColor)
        assertEquals(3, txt.borderWidth)

        val toolbar = checkNotNull(uiCustomization.toolbarCustomization).cardinalToolbarCustomization
        assertEquals("Checkout", toolbar.headerText)
        assertEquals("Done", toolbar.buttonText)
    }

    @Test
    fun cardinalUiCustomization_withNullsAndButtonWithoutType_doesNotCrash() {
        val empty = ThreeDSecureV2UiCustomization()
        assertNotNull(empty.cardinalUiCustomization)

        val buttonNoType = ThreeDSecureV2UiCustomization(
            buttonCustomization = ThreeDSecureV2ButtonCustomization(backgroundColor = "#D3D3D3"),
            buttonType = null
        )
        assertNotNull(buttonNoType.cardinalUiCustomization)
    }
}
