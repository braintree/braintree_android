package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.cardfields.CardBrand
import org.junit.Rule
import org.junit.Test

class CardNumberFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(resId: Int) = context.getString(resId)

    private fun cardNumberField() =
        composeTestRule.onNodeWithContentDescription(str(R.string.card_number_accessibility))

    private fun setCardNumberField() {
        composeTestRule.setContent {
            var value by remember { mutableStateOf(TextFieldValue("")) }
            CardNumberField(
                value = value,
                onValueChange = { value = it },
                brand = CardBrand.UNKNOWN,
                errorText = null
            )
        }
    }

    private fun setCardFields() {
        composeTestRule.setContent {
            CardFields(controller = rememberCardFieldsController())
        }
    }

    private fun assertBrandDetected(prefix: String, iconContentDescriptionRes: Int) {
        setCardFields()

        cardNumberField().performTextInput(prefix)

        composeTestRule.onNodeWithContentDescription(str(iconContentDescriptionRes))
            .assertIsDisplayed()
    }

    @Test
    fun typingDigits_updatesFieldText() {
        setCardNumberField()

        cardNumberField().performTextInput("4111")

        cardNumberField().assert(hasText("4111"))
    }

    @Test
    fun typingVisaPrefix_detectsVisa() =
        assertBrandDetected("4", R.string.card_icon_visa)

    @Test
    fun typingMastercardPrefix_detectsMastercard() =
        assertBrandDetected("51", R.string.card_icon_mastercard)

    @Test
    fun typingAmexPrefix_detectsAmex() =
        assertBrandDetected("34", R.string.card_icon_amex)

    @Test
    fun typingDinersClubPrefix_detectsDinersClub() =
        assertBrandDetected("36", R.string.card_icon_diners_club)

    @Test
    fun typingDiscoverPrefix_detectsDiscover() =
        assertBrandDetected("6011", R.string.card_icon_discover)

    @Test
    fun typingJcbPrefix_detectsJcb() =
        assertBrandDetected("35", R.string.card_icon_jcb)

    @Test
    fun typingUnionPayPrefix_detectsUnionPay() =
        assertBrandDetected("621", R.string.card_icon_unionpay)

    @Test
    fun typingMaestroPrefix_detectsMaestro() =
        assertBrandDetected("56", R.string.card_icon_maestro)

    @Test
    fun typingHiperPrefix_detectsHiper() =
        assertBrandDetected("637095", R.string.card_icon_hiper)

    @Test
    fun typingHipercardPrefix_detectsHipercard() =
        assertBrandDetected("606282", R.string.card_icon_hipercard)

    @Test
    fun typingEloPrefix_detectsElo() =
        assertBrandDetected("636297", R.string.card_icon_elo)

    @Test
    fun typingMirPrefix_detectsMir() =
        assertBrandDetected("2200", R.string.card_icon_mir)

    @Test
    fun typingVervePrefix_detectsVerve() =
        assertBrandDetected("506099", R.string.card_icon_verve)
}
