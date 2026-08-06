package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CardFieldsVisualTransformationsUnitTest {

    // region CardNumberVisualTransformation

    @Test
    fun `formats a Visa number with a 4-4-4-4 gap pattern`() {
        val transformed = CardNumberVisualTransformation(intArrayOf(4, 8, 12)).filter(
            AnnotatedString("4111111111111111")
        )

        assertEquals("4111 1111 1111 1111", transformed.text.text)
    }

    @Test
    fun `formats an Amex number with a 4-6 gap pattern`() {
        val transformed = CardNumberVisualTransformation(intArrayOf(4, 10)).filter(
            AnnotatedString("378282246310005")
        )

        assertEquals("3782 822463 10005", transformed.text.text)
    }

    @Test
    fun `maps an original offset in the middle of the digits to the formatted offset after a space`() {
        val transformed = CardNumberVisualTransformation(intArrayOf(4, 8, 12)).filter(
            AnnotatedString("411111111111")
        )

        // 8 raw digits typed -> lands right after the second space in "4111 1111 1111"
        assertEquals(9, transformed.offsetMapping.originalToTransformed(8))
    }

    @Test
    fun `maps a formatted offset back to the raw digit count before it`() {
        val transformed = CardNumberVisualTransformation(intArrayOf(4, 8, 12)).filter(
            AnnotatedString("411111111111")
        )

        assertEquals(8, transformed.offsetMapping.transformedToOriginal(9))
    }

    // endregion

    // region ExpirationVisualTransformation

    @Test
    fun `formats MMYY digits with a slash after the month`() {
        val transformed = ExpirationVisualTransformation().filter(AnnotatedString("1226"))

        assertEquals("12/26", transformed.text.text)
    }

    @Test
    fun `leaves a partial month unformatted`() {
        val transformed = ExpirationVisualTransformation().filter(AnnotatedString("1"))

        assertEquals("1", transformed.text.text)
    }

    @Test
    fun `maps the original offset after the month to just before the slash`() {
        val transformed = ExpirationVisualTransformation().filter(AnnotatedString("1226"))

        assertEquals(2, transformed.offsetMapping.originalToTransformed(2))
    }

    @Test
    fun `maps a formatted offset after the slash back to two raw digits`() {
        val transformed = ExpirationVisualTransformation().filter(AnnotatedString("1226"))

        assertEquals(2, transformed.offsetMapping.transformedToOriginal(3))
    }

    // endregion
}
