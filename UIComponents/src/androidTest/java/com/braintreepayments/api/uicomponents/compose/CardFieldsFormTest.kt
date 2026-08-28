package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.uicomponents.R
import org.junit.Rule
import org.junit.Test

/**
 * Cross-field behavior owned by the [CardFields] composable + `CardFieldsViewModel`,
 * so every test here renders the whole form via [rememberCardFieldsController]. Two behaviors are covered:
 *
 * 1. **Auto-advance:** when a field becomes valid while it holds focus, focus jumps to the next field.
 * 2. **Validation state machine :** while the user is typing, an incomplete value
 *    shows NO error (it stays `Validating`); only when focus leaves the field (blur) does an
 *    incomplete value resolve to an error — "required" if empty, or "invalid" if partial.
 */
class CardFieldsFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(resId: Int) = context.getString(resId)

    private fun setCardFields() {
        composeTestRule.setContent {
            CardFields(controller = rememberCardFieldsController())
        }
    }

    private fun cvvField() =
        composeTestRule.onNodeWithContentDescription(str(R.string.cvv_accessibility))

    private fun cardNumberField() =
        composeTestRule.onNodeWithContentDescription(str(R.string.card_number_accessibility))

    private fun expirationField() =
        composeTestRule.onNodeWithContentDescription(str(R.string.expiration_hint_accessibility))

    @Test
    fun typingValidCardNumber_advancesFocusToExpiration() {
        setCardFields()

        cardNumberField().performTextInput("4111111111111111")

        expirationField().assertIsFocused()
    }

    @Test
    fun typingValidExpiration_advancesFocusToCvv() {
        setCardFields()

        expirationField().performTextInput("1230")

        cvvField().assertIsFocused()
    }

    @Test
    fun typingPartialCvvThenLosingFocus_showsInvalidError() {
        setCardFields()

        cvvField().performTextInput("2")

        cardNumberField().performClick()

        composeTestRule.onNodeWithText(str(R.string.cvv_error)).assertIsDisplayed()
    }

    @Test
    fun losingFocusOnEmptyCvv_showsRequiredError() {
        setCardFields()

        cvvField().performClick()
        cardNumberField().performClick()

        composeTestRule.onNodeWithText(str(R.string.cvv_required)).assertIsDisplayed()
    }

    @Test
    fun typingPartialCvvWithoutLosingFocus_showsNoError() {
        setCardFields()

        cvvField().performTextInput("2")

        composeTestRule.onNodeWithText(str(R.string.cvv_error)).assertDoesNotExist()
    }

    @Test
    fun typingPartialCardNumberThenLosingFocus_showsInvalidError() {
        setCardFields()

        cardNumberField().performTextInput("4111")
        cvvField().performClick()

        composeTestRule.onNodeWithText(str(R.string.card_number_error)).assertIsDisplayed()
    }

    @Test
    fun losingFocusOnEmptyCardNumber_showsRequiredError() {
        setCardFields()

        cardNumberField().performClick()
        cvvField().performClick()

        composeTestRule.onNodeWithText(str(R.string.card_number_required)).assertIsDisplayed()
    }

    @Test
    fun typingPartialExpirationThenLosingFocus_showsInvalidError() {
        setCardFields()

        expirationField().performTextInput("12")
        cardNumberField().performClick()

        composeTestRule.onNodeWithText(str(R.string.expiration_error)).assertIsDisplayed()
    }

    @Test
    fun losingFocusOnEmptyExpiration_showsRequiredError() {
        setCardFields()

        expirationField().performClick()
        cardNumberField().performClick()

        composeTestRule.onNodeWithText(str(R.string.expiration_required)).assertIsDisplayed()
    }
}
