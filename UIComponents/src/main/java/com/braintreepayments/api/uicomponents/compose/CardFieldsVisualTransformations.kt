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

/**
 * Masks every CVV digit except the one at [revealedIndex], which is shown in the clear.
 * Mirrors the brief reveal-then-mask behavior the XML [com.braintreepayments.api.uicomponents.cardfields.CvvTextInputView]
 * gets for free from `InputType.TYPE_NUMBER_VARIATION_PASSWORD`'s platform `PasswordTransformationMethod`.
 * Length never changes, so the offset mapping is the identity mapping.
 */
internal class CvvVisualTransformation(private val revealedIndex: Int?) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val masked = text.text.mapIndexed { index, char ->
            if (index == revealedIndex) char else MASK_CHAR
        }.joinToString("")
        return TransformedText(AnnotatedString(masked), OffsetMapping.Identity)
    }

    private companion object {
        const val MASK_CHAR = '•'
    }
}
