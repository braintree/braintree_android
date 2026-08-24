package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.braintreepayments.api.uicomponents.cardfields.CardNumberFormatter

internal class CardNumberVisualTransformation(private val formatGaps: IntArray) :
    VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = CardNumberFormatter.formatCardNumber(text.text, formatGaps)
        return TransformedText(AnnotatedString(formatted), digitOffsetMapping(formatted))
    }
}

private fun digitOffsetMapping(formatted: String) = object : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int =
        CardNumberFormatter.findIndexForDigitPosition(formatted, offset)

    override fun transformedToOriginal(offset: Int): Int =
        CardNumberFormatter.countDigitsBeforeIndex(formatted, offset)
}
