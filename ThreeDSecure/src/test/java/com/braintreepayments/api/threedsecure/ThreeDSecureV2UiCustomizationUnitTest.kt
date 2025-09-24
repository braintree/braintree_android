package com.braintreepayments.api.threedsecure

import android.os.Parcel
import com.cardinalcommerce.shared.models.enums.ButtonType
import kotlinx.parcelize.parcelableCreator
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureV2UiCustomizationUnitTest {

    @Test
    fun `sets all cardinal class properties`() {
        val sut = ThreeDSecureV2UiCustomization(
            labelCustomization = ThreeDSecureV2LabelCustomization(),
            buttonCustomization = ThreeDSecureV2ButtonCustomization(),
            buttonType = ThreeDSecureV2ButtonType.BUTTON_TYPE_NEXT,
            textBoxCustomization = ThreeDSecureV2TextBoxCustomization(),
            toolbarCustomization = ThreeDSecureV2ToolbarCustomization()
        )

        val cardinalUiCustomization = sut.cardinalUiCustomization
        assertNotNull(cardinalUiCustomization.labelCustomization)
        assertNotNull(cardinalUiCustomization.getButtonCustomization(ButtonType.NEXT))
        assertNotNull(cardinalUiCustomization.textBoxCustomization)
        assertNotNull(cardinalUiCustomization.toolbarCustomization)
    }

    @Test
    fun `constructor defaults UiCustomization property to empty object`() {
        val sut = ThreeDSecureV2UiCustomization()
        assertNotNull(sut.cardinalUiCustomization)
    }

    @Suppress("LongMethod")
    @Test
    fun `write to parcel`() {
        val buttonCustomization = ThreeDSecureV2ButtonCustomization(
            backgroundColor = "#FF0000",
            cornerRadius = 5,
            textColor = "#000000",
            textFontName = "Comic Sans",
            textFontSize = 20
        )

        val labelCustomization = ThreeDSecureV2LabelCustomization(
            headingTextColor = "#FFFFFF",
            headingTextFontName = "Times New Roman",
            headingTextFontSize = 30,
            textColor = "#121212",
            textFontName = "Helvetica",
            textFontSize = 15
        )

        val textBoxCustomization = ThreeDSecureV2TextBoxCustomization(
            borderColor = "#FFFFFF",
            borderWidth = 10,
            cornerRadius = 5,
            textColor = "#121212",
            textFontName = "Helvetica",
            textFontSize = 15
        )

        val toolbarCustomization = ThreeDSecureV2ToolbarCustomization(
            backgroundColor = "#FFFFFF",
            buttonText = "Button",
            headerText = "Header",
            textColor = "#121212",
            textFontName = "Helvetica",
            textFontSize = 15
        )

        val sut = ThreeDSecureV2UiCustomization(
            buttonCustomization = buttonCustomization,
            buttonType = ThreeDSecureV2ButtonType.BUTTON_TYPE_CONTINUE,
            labelCustomization = labelCustomization,
            textBoxCustomization = textBoxCustomization,
            toolbarCustomization = toolbarCustomization
        )

        val parcel = Parcel.obtain().apply {
            sut.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val parceled = parcelableCreator<ThreeDSecureV2UiCustomization>().createFromParcel(parcel)

        assertEquals("#FF0000", parceled.buttonCustomization?.backgroundColor)
        assertEquals(5, parceled.buttonCustomization?.cornerRadius)
        assertEquals("#000000", parceled.buttonCustomization?.textColor)
        assertEquals("Comic Sans", parceled.buttonCustomization?.textFontName)
        assertEquals(20, parceled.buttonCustomization?.textFontSize)

        assertEquals("#FF0000",
            parceled.cardinalUiCustomization.getButtonCustomization(ButtonType.CONTINUE).backgroundColor)
        assertEquals(5,
            parceled.cardinalUiCustomization.getButtonCustomization(ButtonType.CONTINUE).cornerRadius)
        assertEquals("#000000",
            parceled.cardinalUiCustomization.getButtonCustomization(ButtonType.CONTINUE).textColor)
        assertEquals("Comic Sans",
            parceled.cardinalUiCustomization.getButtonCustomization(ButtonType.CONTINUE).textFontName)
        assertEquals(20,
            parceled.cardinalUiCustomization.getButtonCustomization(ButtonType.CONTINUE).textFontSize)

        assertEquals("#FFFFFF", parceled.labelCustomization?.headingTextColor)
        assertEquals("Times New Roman", parceled.labelCustomization?.headingTextFontName)
        assertEquals(30, parceled.labelCustomization?.headingTextFontSize)
        assertEquals("#121212", parceled.labelCustomization?.textColor)
        assertEquals("Helvetica", parceled.labelCustomization?.textFontName)
        assertEquals(15, parceled.labelCustomization?.textFontSize)

        assertEquals("#FFFFFF",
            parceled.cardinalUiCustomization.labelCustomization.headingTextColor)
        assertEquals("Times New Roman",
            parceled.cardinalUiCustomization.labelCustomization.headingTextFontName)
        assertEquals(30,
            parceled.cardinalUiCustomization.labelCustomization.headingTextFontSize)
        assertEquals("#121212",
            parceled.cardinalUiCustomization.labelCustomization.textColor)
        assertEquals("Helvetica",
            parceled.cardinalUiCustomization.labelCustomization.textFontName)
        assertEquals(15,
            parceled.cardinalUiCustomization.labelCustomization.textFontSize)

        assertEquals("#FFFFFF", parceled.textBoxCustomization?.borderColor)
        assertEquals(10, parceled.textBoxCustomization?.borderWidth)
        assertEquals(5, parceled.textBoxCustomization?.cornerRadius)
        assertEquals("#121212", parceled.textBoxCustomization?.textColor)
        assertEquals("Helvetica", parceled.textBoxCustomization?.textFontName)
        assertEquals(15, parceled.textBoxCustomization?.textFontSize)

        assertEquals("#FFFFFF",
            parceled.cardinalUiCustomization.textBoxCustomization.borderColor)
        assertEquals(10,
            parceled.cardinalUiCustomization.textBoxCustomization.borderWidth)
        assertEquals(5,
            parceled.cardinalUiCustomization.textBoxCustomization.cornerRadius)
        assertEquals("#121212",
            parceled.cardinalUiCustomization.textBoxCustomization.textColor)
        assertEquals("Helvetica",
            parceled.cardinalUiCustomization.textBoxCustomization.textFontName)
        assertEquals(15,
            parceled.cardinalUiCustomization.textBoxCustomization.textFontSize)

        assertEquals("#FFFFFF", parceled.toolbarCustomization?.backgroundColor)
        assertEquals("Button", parceled.toolbarCustomization?.buttonText)
        assertEquals("Header", parceled.toolbarCustomization?.headerText)
        assertEquals("#121212", parceled.toolbarCustomization?.textColor)
        assertEquals("Helvetica", parceled.toolbarCustomization?.textFontName)
        assertEquals(15, parceled.toolbarCustomization?.textFontSize)

        assertEquals("#FFFFFF",
            parceled.cardinalUiCustomization.toolbarCustomization.backgroundColor)
        assertEquals("Button",
            parceled.cardinalUiCustomization.toolbarCustomization.buttonText)
        assertEquals("Header",
            parceled.cardinalUiCustomization.toolbarCustomization.headerText)
        assertEquals("#121212",
            parceled.cardinalUiCustomization.toolbarCustomization.textColor)
        assertEquals("Helvetica",
            parceled.cardinalUiCustomization.toolbarCustomization.textFontName)
        assertEquals(15,
            parceled.cardinalUiCustomization.toolbarCustomization.textFontSize)
    }
}
