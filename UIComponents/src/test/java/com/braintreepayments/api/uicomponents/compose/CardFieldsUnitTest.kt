package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
        val result = sanitizeCvvInput(textFieldValue("1a2b3"))
        assertEquals("123", result?.text)
    }

    @Test
    fun `sanitizeCvvInput allows digits under the max length for an unrecognized brand`() {
        val result = sanitizeCvvInput(textFieldValue("99"))
        assertEquals("99", result?.text)
    }

    @Test
    fun `sanitizeCvvInput allows digits at exactly the max length for an unrecognized brand`() {
        val result = sanitizeCvvInput(textFieldValue("9999"))
        assertEquals("9999", result?.text)
    }

    @Test
    fun `sanitizeCvvInput rejects a single extra digit past the max length for an unrecognized brand`() {
        val result = sanitizeCvvInput(textFieldValue("99999"))
        assertNull(result)
    }

    @Test
    fun `sanitizeCvvInput allows an empty value`() {
        val result = sanitizeCvvInput(textFieldValue(""))
        assertEquals("", result?.text)
    }

    @Test
    fun `sanitizeCvvInput respects a shorter max length for digits matching a known brand prefix`() {
        val result = sanitizeCvvInput(textFieldValue("411"))
        assertEquals("411", result?.text)
    }

    @Test
    fun `sanitizeCvvInput rejects a digit past the max length for digits matching a known brand prefix`() {
        val result = sanitizeCvvInput(textFieldValue("4111"))
        assertNull(result)
    }

    // endregion
}
