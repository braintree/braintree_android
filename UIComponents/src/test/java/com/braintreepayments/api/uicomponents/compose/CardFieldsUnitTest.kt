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
}
