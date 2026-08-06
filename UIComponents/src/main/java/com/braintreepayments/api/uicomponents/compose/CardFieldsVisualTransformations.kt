package com.braintreepayments.api.uicomponents.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.braintreepayments.api.uicomponents.cardfields.CardNumberFormatter
import com.braintreepayments.api.uicomponents.cardfields.ExpirationDateFormatter

/**
 * Formats raw card number digits with spaces at [formatGaps]. The underlying [TextFieldValue][
 * androidx.compose.ui.text.input.TextFieldValue] stays raw digits; only the rendered text and
 * cursor position are transformed, so callers can read [AnnotatedString.text] directly to get the
 * raw card number.
 */
internal class CardNumberVisualTransformation(private val formatGaps: IntArray) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = CardNumberFormatter.formatCardNumber(text.text, formatGaps)
        return TransformedText(AnnotatedString(formatted), digitOffsetMapping(formatted))
    }
}

/** Formats raw MMYY digits as "MM/YY". See [CardNumberVisualTransformation] for the raw-value contract. */
internal class ExpirationVisualTransformation : VisualTransformation {
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
