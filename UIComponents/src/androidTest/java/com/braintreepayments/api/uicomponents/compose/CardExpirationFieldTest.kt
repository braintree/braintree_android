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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.uicomponents.R
import org.junit.Rule
import org.junit.Test

class CardExpirationFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(resId: Int) = context.getString(resId)

    /**
     * Mirrors how [CardFields] wires this field in production: raw input is run through
     * [sanitizeCardExpirationInput] before being written back into state.
     */
    private fun setExpirationField(errorText: String? = null) {
        composeTestRule.setContent {
            var value by remember { mutableStateOf(TextFieldValue("")) }
            CardExpirationField(
                value = value,
                onValueChange = { newValue ->
                    value = sanitizeCardExpirationInput(newValue) ?: value
                },
                errorText = errorText
            )
        }
    }

    @Test
    fun typingDigits_updatesFieldText() {
        setExpirationField()

        composeTestRule.onNodeWithContentDescription(str(R.string.expiration_hint_accessibility))
            .performTextInput("1225")

        composeTestRule.onNodeWithContentDescription(str(R.string.expiration_hint_accessibility))
            .assert(hasText("1225"))
    }

    @Test
    fun typingSingleDigitMonthGreaterThanOne_appliesLeadingZero() {
        setExpirationField()
        val expirationInput =
            composeTestRule.onNodeWithContentDescription(str(R.string.expiration_hint_accessibility))

        // Typed one character at a time, matching real keystrokes: sanitizeCardExpirationInput
        // only inserts the leading zero while exactly one digit has been entered so far.
        expirationInput.performTextInput("2")
        expirationInput.performTextInput("2")
        expirationInput.performTextInput("5")

        expirationInput.assert(hasText("0225"))
    }

    @Test
    fun typingMoreThanFourDigits_isRejected() {
        setExpirationField()

        composeTestRule.onNodeWithContentDescription(str(R.string.expiration_hint_accessibility))
            .performTextInput("12345")

        composeTestRule.onNodeWithContentDescription(str(R.string.expiration_hint_accessibility))
            .assert(hasText(""))
    }

    @Test
    fun errorText_isDisplayed_whenProvided() {
        setExpirationField(errorText = "Expiration date is invalid")

        composeTestRule.onNodeWithText("Expiration date is invalid").assertIsDisplayed()
    }

    @Test
    fun errorText_isNotDisplayed_whenNull() {
        setExpirationField(errorText = null)

        composeTestRule.onNodeWithText("Expiration date is invalid").assertDoesNotExist()
    }
}
