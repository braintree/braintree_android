package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.uicomponents.R
import org.junit.Rule
import org.junit.Test

/**
 * Cross-field behavior owned by the [CardFields] composable + `CardFieldsViewModel`,
 * so every test here renders the whole form via [rememberCardFieldsController]. Four behaviors are covered:
 *
 * 1. **Auto-advance:** when a field becomes valid while it holds focus, focus jumps to the next field.
 * 2. **Validation state machine:** while the user is typing, an incomplete value
 *    shows NO error (it stays `Validating`); only when focus leaves the field (blur) does an
 *    incomplete value resolve to an error — "required" if empty, or "invalid" if partial.
 * 3. **CVV length cap:** the CVV is capped at the detected brand's `cvvLength` (3 for most brands,
 *    4 for Amex).
 * 4. **Form validity gating a pay button:** `CardFieldsController.isFormValid` emits `false` until all
 *    three fields are valid, then `true`, and flips back to `false` if a valid field is later changed
 *    to an invalid value.
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

    private fun setCardFieldsWithPayButton() {
        composeTestRule.setContent {
            val controller = rememberCardFieldsController()
            val isFormValid by controller.isFormValid.collectAsState()
            Column {
                CardFields(controller = controller)
                Button(onClick = {}, enabled = isFormValid) {
                    Text("Pay")
                }
            }
        }
    }

    private fun cvvField() =
        composeTestRule.onNodeWithContentDescription(str(R.string.cvv_accessibility))

    private fun cardNumberField() =
        composeTestRule.onNodeWithContentDescription(str(R.string.card_number_accessibility))

    private fun expirationField() =
        composeTestRule.onNodeWithContentDescription(str(R.string.expiration_hint_accessibility))

    private fun payButton() = composeTestRule.onNodeWithText("Pay")

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

    @Test
    fun typingMoreThanMaxCvvDigits_rejectsExtraDigit_ThreeDigitBrand() {
        setCardFields()

        // A "4" prefix detects Visa, whose CVV max length is 3.
        cardNumberField().performTextInput("4")

        cvvField().performTextInput("123")
        cvvField().performTextInput("4")
        cvvField().assert(hasText("123"))
    }

    @Test
    fun typingMoreThanMaxCvvDigits_rejectsExtraDigit_Amex() {
        setCardFields()

        // A "34" prefix detects Amex, whose CVV max length is 4.
        cardNumberField().performTextInput("34")

        cvvField().performTextInput("1234")
        cvvField().performTextInput("5")
        cvvField().assert(hasText("1234"))
    }

    @Test
    fun payButton_isInitiallyDisabled() {
        setCardFieldsWithPayButton()

        payButton().assertIsNotEnabled()
    }

    @Test
    fun payButton_isEnabledAfterAllFieldsAreValid() {
        setCardFieldsWithPayButton()

        cardNumberField().performTextInput("4111111111111111")
        expirationField().performTextInput("1230")
        cvvField().performTextInput("123")

        payButton().assertIsEnabled()
    }

    @Test
    fun payButton_becomesDisabledAgain_whenAValidFieldIsChangedToInvalid() {
        setCardFieldsWithPayButton()

        cardNumberField().performTextInput("4111111111111111")
        expirationField().performTextInput("1230")
        cvvField().performTextInput("123")
        payButton().assertIsEnabled()

        cvvField().performTextClearance()
        cvvField().performTextInput("12")

        payButton().assertIsNotEnabled()
    }
}
