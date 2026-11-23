package com.braintreepayments.api.uicomponents.compose

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.cardfields.CardBrand
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardFieldsUnitTest {

    private fun textFieldValue(text: String, cursor: Int = text.length) =
        TextFieldValue(text = text, selection = TextRange(cursor))

    // region sanitizeCardNumberInput

    @Test
    fun `sanitizeCardNumberInput strips non-digit characters`() {
        val result = sanitizeCardNumberInput(textFieldValue("4111 1111-1111 1111"))
        assertEquals("4111111111111111", result?.text)
    }

    @Test
    fun `sanitizeCardNumberInput allows digits under the detected brand max length`() {
        val result = sanitizeCardNumberInput(textFieldValue("411111"))
        assertEquals("411111", result?.text)
    }

    @Test
    fun `sanitizeCardNumberInput allows digits at exactly the detected brand max length`() {
        val visaNumber = "4" + "1".repeat(15)
        val result = sanitizeCardNumberInput(textFieldValue(visaNumber))
        assertEquals(visaNumber, result?.text)
    }

    @Test
    fun `sanitizeCardNumberInput rejects a single extra digit appended past max length`() {
        val visaMax = "4" + "1".repeat(15)
        val overflowed = visaMax + "1"
        val result = sanitizeCardNumberInput(textFieldValue(overflowed))
        assertNull(result)
    }

    @Test
    fun `sanitizeCardNumberInput rejects a single extra digit inserted in the middle past max length`() {
        val visaMax = "4" + "1".repeat(15)
        val overflowed = visaMax.substring(0, 8) + "9" + visaMax.substring(8)
        val result = sanitizeCardNumberInput(textFieldValue(overflowed, cursor = 9))
        assertNull(result)
    }

    @Test
    fun `sanitizeCardNumberInput rejects a large paste that exceeds max length`() {
        val visaMax = "4" + "1".repeat(15)
        val pastedInMiddle = visaMax.substring(0, 4) + "222222222" + visaMax.substring(4)
        val result = sanitizeCardNumberInput(textFieldValue(pastedInMiddle, cursor = 13))
        assertNull(result)
    }

    @Test
    fun `sanitizeCardNumberInput respects Amex's shorter max length`() {
        val amexMax = "34" + "1".repeat(13)
        val overflowed = amexMax + "1"
        val result = sanitizeCardNumberInput(textFieldValue(overflowed))
        assertNull(result)
    }

    @Test
    fun `sanitizeCardNumberInput allows an empty value`() {
        val result = sanitizeCardNumberInput(textFieldValue(""))
        assertEquals("", result?.text)
    }

    // endregion

    // region sanitizeCardExpirationInput

    @Test
    fun `sanitizeCardExpirationInput strips non-digit characters`() {
        val result = sanitizeCardExpirationInput(textFieldValue("12/25"))
        assertEquals("1225", result?.text)
    }

    @Test
    fun `sanitizeCardExpirationInput allows digits under the max length`() {
        val result = sanitizeCardExpirationInput(textFieldValue("122"))
        assertEquals("122", result?.text)
    }

    @Test
    fun `sanitizeCardExpirationInput allows digits at exactly the max length`() {
        val result = sanitizeCardExpirationInput(textFieldValue("1225"))
        assertEquals("1225", result?.text)
    }

    @Test
    fun `sanitizeCardExpirationInput rejects a single extra digit appended past max length`() {
        val result = sanitizeCardExpirationInput(textFieldValue("12255"))
        assertNull(result)
    }

    @Test
    fun `sanitizeCardExpirationInput rejects a large paste that exceeds max length`() {
        val result = sanitizeCardExpirationInput(textFieldValue("1229999925"))
        assertNull(result)
    }

    @Test
    fun `sanitizeCardExpirationInput allows an empty value`() {
        val result = sanitizeCardExpirationInput(textFieldValue(""))
        assertEquals("", result?.text)
    }

    @Test
    fun `sanitizeCardExpirationInput prepends a leading zero for a lone month digit greater than 1`() {
        val result = sanitizeCardExpirationInput(textFieldValue("5"))
        assertEquals("05", result?.text)
    }

    @Test
    fun `sanitizeCardExpirationInput shifts the cursor forward when a leading zero is inserted`() {
        val result = sanitizeCardExpirationInput(textFieldValue("5", cursor = 1))
        assertEquals(TextRange(2), result?.selection)
    }

    @Test
    fun `sanitizeCardExpirationInput does not prepend a leading zero for a lone 0 or 1`() {
        assertEquals("0", sanitizeCardExpirationInput(textFieldValue("0"))?.text)
        assertEquals("1", sanitizeCardExpirationInput(textFieldValue("1"))?.text)
    }

    @Test
    fun `sanitizeCardExpirationInput does not prepend a leading zero once a second digit is present`() {
        val result = sanitizeCardExpirationInput(textFieldValue("51"))
        assertEquals("51", result?.text)
    }

    @Test
    fun `sanitizeCardExpirationInput leaves the cursor unchanged when no leading zero is inserted`() {
        val result = sanitizeCardExpirationInput(textFieldValue("12", cursor = 1))
        assertEquals(TextRange(1), result?.selection)
    }

    // endregion

    // region sanitizeCvvInput

    @Test
    fun `sanitizeCvvInput strips non-digit characters`() {
        val result = sanitizeCvvInput(textFieldValue("1a2b3"), CardBrand.UNKNOWN)
        assertEquals("123", result?.text)
    }

    @Test
    fun `sanitizeCvvInput allows digits under the max length for an unrecognized brand`() {
        val result = sanitizeCvvInput(textFieldValue("99"), CardBrand.UNKNOWN)
        assertEquals("99", result?.text)
    }

    @Test
    fun `sanitizeCvvInput allows digits at exactly the max length for an unrecognized brand`() {
        val result = sanitizeCvvInput(textFieldValue("9999"), CardBrand.UNKNOWN)
        assertEquals("9999", result?.text)
    }

    @Test
    fun `sanitizeCvvInput rejects a single extra digit past the max length for an unrecognized brand`() {
        val result = sanitizeCvvInput(textFieldValue("99999"), CardBrand.UNKNOWN)
        assertNull(result)
    }

    @Test
    fun `sanitizeCvvInput allows an empty value`() {
        val result = sanitizeCvvInput(textFieldValue(""), CardBrand.UNKNOWN)
        assertEquals("", result?.text)
    }

    @Test
    fun `sanitizeCvvInput respects the detected brand's shorter max length`() {
        val result = sanitizeCvvInput(textFieldValue("411"), CardBrand.VISA)
        assertEquals("411", result?.text)
    }

    @Test
    fun `sanitizeCvvInput rejects a digit past the detected brand's max length`() {
        val result = sanitizeCvvInput(textFieldValue("4111"), CardBrand.VISA)
        assertNull(result)
    }

    // endregion

    // region CardFields composable CVV truncation

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)

    private fun String.asTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

    private fun controllerWith(cardNumber: String, cvv: String): CardFieldsController {
        val viewModel = CardFieldsViewModel()
        viewModel.onCardNumberChanged(cardNumber)
        viewModel.onCvvChanged(cvv)
        return CardFieldsController(
            viewModel,
            mutableStateOf(cardNumber.asTextFieldValue()),
            mutableStateOf("".asTextFieldValue()),
            mutableStateOf(cvv.asTextFieldValue()),
            mockk<CardClient>(relaxed = true),
            analyticsClient = analyticsClient
        )
    }

    private fun cardNumberField() =
        composeRule.onNodeWithContentDescription(context.getString(R.string.card_number_accessibility))

    @Test
    fun `changing from Amex to Visa truncates a 4 digit CVV to 3 digits`() {
        val controller = controllerWith(cardNumber = "378282246310005", cvv = "1234")
        composeRule.setContent { CardFields(controller = controller) }

        cardNumberField().performTextReplacement("4111111111111111")
        composeRule.waitForIdle()

        assertEquals("123", controller.cvv.value.text)
        assertEquals(TextRange(3), controller.cvv.value.selection)
        assertEquals("123", controller.viewModel.currentCvv)
    }

    @Test
    fun `changing between brands with the same CVV length leaves the CVV untouched`() {
        val controller = controllerWith(cardNumber = "4111111111111111", cvv = "123")
        composeRule.setContent { CardFields(controller = controller) }

        cardNumberField().performTextReplacement("5555555555554444")
        composeRule.waitForIdle()

        assertEquals("123", controller.cvv.value.text)
    }

    @Test
    fun `a 4 digit CVV is kept when the card stays Amex`() {
        val controller = controllerWith(cardNumber = "378282246310005", cvv = "1234")
        composeRule.setContent { CardFields(controller = controller) }

        cardNumberField().performTextReplacement("371449635398431")
        composeRule.waitForIdle()

        assertEquals("1234", controller.cvv.value.text)
    }

    // endregion
}
