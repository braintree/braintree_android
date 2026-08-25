package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CardFieldsVisualTransformationUnitTest {

    // region cardNumberVisualTransformationTests
    private val visaFormatGaps = intArrayOf(4, 8, 12)
    private val amexFormatGaps = intArrayOf(4, 10)

    @Test
    fun `filter formats digits with spaces at Visa gap positions`() {
        val transformation = CardNumberVisualTransformation(visaFormatGaps)
        val result = transformation.filter(AnnotatedString("4111111111111111"))
        assertEquals("4111 1111 1111 1111", result.text.text)
    }

    @Test
    fun `filter formats digits with spaces at Amex gap positions`() {
        val transformation = CardNumberVisualTransformation(amexFormatGaps)
        val result = transformation.filter(AnnotatedString("341111111111111"))
        assertEquals("3411 111111 11111", result.text.text)
    }

    @Test
    fun `filter returns empty text for empty input`() {
        val transformation = CardNumberVisualTransformation(visaFormatGaps)
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun `filter does not insert spaces beyond the entered digits`() {
        val transformation = CardNumberVisualTransformation(visaFormatGaps)
        val result = transformation.filter(AnnotatedString("41"))
        assertEquals("41", result.text.text)
    }

    @Test
    fun `originalToTransformed maps a digit index to the position after inserted spaces`() {
        val transformation = CardNumberVisualTransformation(visaFormatGaps)
        val result = transformation.filter(AnnotatedString("4111111111111111"))
        // The 5th digit ("1" right after the first gap) lands after the inserted space.
        assertEquals(6, result.offsetMapping.originalToTransformed(5))
    }

    @Test
    fun `transformedToOriginal maps a formatted position back to the raw digit count`() {
        val transformation = CardNumberVisualTransformation(visaFormatGaps)
        val result = transformation.filter(AnnotatedString("4111111111111111"))
        val formattedLength = result.text.text.length
        assertEquals(16, result.offsetMapping.transformedToOriginal(formattedLength))
    }

    // endregion

    // region expirationDateVisualTransformationTests

    private val expirationDateTransformation = ExpirationDateVisualTransformation()

    @Test
    fun `filter inserts a slash between month and year digits`() {
        val result = expirationDateTransformation.filter(AnnotatedString("1225"))
        assertEquals("12/25", result.text.text)
    }

    @Test
    fun `filter does not insert a slash for two or fewer digits`() {
        val result = expirationDateTransformation.filter(AnnotatedString("12"))
        assertEquals("12", result.text.text)
    }

    @Test
    fun `filter returns empty text for empty expiration date input`() {
        val result = expirationDateTransformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun `originalToTransformed maps a digit index to the position after the inserted slash`() {
        val result = expirationDateTransformation.filter(AnnotatedString("1225"))
        // The 3rd digit ("2" right after the slash) lands after the inserted slash.
        assertEquals(4, result.offsetMapping.originalToTransformed(3))
    }

    @Test
    fun `transformedToOriginal maps a formatted expiration date position back to the raw digit count`() {
        val result = expirationDateTransformation.filter(AnnotatedString("1225"))
        val formattedLength = result.text.text.length
        assertEquals(4, result.offsetMapping.transformedToOriginal(formattedLength))
    }

    // endregion
}
