package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.braintreepayments.api.uicomponents.cardfields.CardNumberFormatter
import com.braintreepayments.api.uicomponents.cardfields.ExpirationDateFormatter

internal class CardNumberVisualTransformation(private val formatGaps: IntArray) :
    VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = CardNumberFormatter.formatCardNumber(text.text, formatGaps)
        return TransformedText(AnnotatedString(formatted), digitOffsetMapping(formatted))
    }
}

internal class ExpirationDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = ExpirationDateFormatter.formatExpiration(text.text)
        return TransformedText(AnnotatedString(formatted), digitOffsetMapping(formatted))
    }
}

private fun digitOffsetMapping(formatted: String) = object : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int =
        CardNumberFormatter.findIndexForDigitPosition(formatted, offset)

    override fun transformedToOriginal(offset: Int): Int =
        CardNumberFormatter.countDigitsBeforeIndex(formatted, offset)
}
