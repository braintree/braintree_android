package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CvvVisualTransformationUnitTest {

    @Test
    fun `filter masks every character when no index is revealed`() {
        val transformation = CvvVisualTransformation(revealedIndex = null)
        val result = transformation.filter(AnnotatedString("123"))
        assertEquals("•••", result.text.text)
    }

    @Test
    fun `filter reveals only the character at the revealed index`() {
        val transformation = CvvVisualTransformation(revealedIndex = 1)
        val result = transformation.filter(AnnotatedString("123"))
        assertEquals("•2•", result.text.text)
    }

    @Test
    fun `filter reveals the last character when the revealed index is the final digit`() {
        val transformation = CvvVisualTransformation(revealedIndex = 3)
        val result = transformation.filter(AnnotatedString("1234"))
        assertEquals("•••4", result.text.text)
    }

    @Test
    fun `filter ignores a revealed index outside the text bounds`() {
        val transformation = CvvVisualTransformation(revealedIndex = 5)
        val result = transformation.filter(AnnotatedString("123"))
        assertEquals("•••", result.text.text)
    }

    @Test
    fun `filter returns empty text for empty input`() {
        val transformation = CvvVisualTransformation(revealedIndex = null)
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun `offset mapping is identity since masking does not change text length`() {
        val transformation = CvvVisualTransformation(revealedIndex = null)
        val result = transformation.filter(AnnotatedString("123"))
        assertEquals(2, result.offsetMapping.originalToTransformed(2))
        assertEquals(2, result.offsetMapping.transformedToOriginal(2))
    }
}
