package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpirationDateVisualTransformationUnitTest {

    private val transformation = ExpirationDateVisualTransformation()

    @Test
    fun `filter inserts a slash between month and year digits`() {
        val result = transformation.filter(AnnotatedString("1225"))
        assertEquals("12/25", result.text.text)
    }

    @Test
    fun `filter does not insert a slash for two or fewer digits`() {
        val result = transformation.filter(AnnotatedString("12"))
        assertEquals("12", result.text.text)
    }

    @Test
    fun `filter returns empty text for empty input`() {
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun `originalToTransformed maps a digit index to the position after the inserted slash`() {
        val result = transformation.filter(AnnotatedString("1225"))
        // The 3rd digit ("2" right after the slash) lands after the inserted slash.
        assertEquals(4, result.offsetMapping.originalToTransformed(3))
    }

    @Test
    fun `transformedToOriginal maps a formatted position back to the raw digit count`() {
        val result = transformation.filter(AnnotatedString("1225"))
        val formattedLength = result.text.text.length
        assertEquals(4, result.offsetMapping.transformedToOriginal(formattedLength))
    }
}