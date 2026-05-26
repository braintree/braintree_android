package com.braintreepayments.api.uicomponents.cardfields

import android.text.Editable
import android.text.InputFilter
import android.text.Selection
import android.text.TextWatcher

/**
 * A [TextWatcher] that formats a card number with spaces at positions defined by the
 * detected [CardBrand]'s [CardBrand.formatGaps].
 *
 * Responsibilities:
 * 1. Detects the card brand from the raw digits on every text change
 * 2. Inserts visual spaces at brand-specific gap positions (e.g., 4-4-4-4 for Visa, 4-6-5 for Amex)
 * 3. Enforces max digit length
 * 4. Preserves the cursor position across formatting changes
 * 5. Notifies the hosting view when the detected brand changes
 *
 */
@Suppress("MagicNumber")
internal class CardNumberFormatter(
    private val onBrandChanged: (CardBrand) -> Unit
) : TextWatcher {

    var currentBrand: CardBrand = CardBrand.UNKNOWN
        private set

    private var isFormatting = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    /**
     * Formats the card number and detects the brand after every text change.
     *
     * The processing pipeline runs in this order:
     * 1. **Strip** — extract raw digits from the editable, ignoring any existing spaces.
     * 2. **Detect** — run [CardBrand.detect] and resolve to a single brand (ambiguous = UNKNOWN).
     *    If the brand changed since the last call, notify via [onBrandChanged].
     * 3. **Truncate** — cap digits at the detected brand's [CardBrand.maxLength].
     * 4. **Format** — rebuild the string with spaces inserted at [CardBrand.formatGaps] positions.
     * 5. **Replace** — if the formatted string differs from the current editable content, swap it in.
     *    Input filters are temporarily cleared during the replace so the [android.text.method.DigitsKeyListener]
     *    filter (installed by [android.text.InputType.TYPE_CLASS_NUMBER]) does not strip the spaces.
     * 6. **Cursor** — restore the cursor to the same logical digit position using a digit-counting
     *    technique: count digits before the cursor pre-format, then find that digit index post-format.
     * 7. **Length filter** — install an [InputFilter.LengthFilter] sized for the brand's max digits
     *    plus the number of inserted spaces, so the soft keyboard cannot exceed the limit.
     *
     * The [isFormatting] flag prevents re-entrant calls.
     */
    override fun afterTextChanged(editable: Editable?) {
        if (isFormatting || editable == null) return
        isFormatting = true

        val digits = editable.toString().filter { it.isDigit() }
        val detected = CardBrand.resolveBrand(digits)

        if (detected != currentBrand) {
            currentBrand = detected
            onBrandChanged(detected)
        }

        val truncated = if (digits.length > detected.maxLength) {
            digits.take(detected.maxLength)
        } else {
            digits
        }

        val formatted = formatCardNumber(truncated, detected.formatGaps)

        if (editable.toString() != formatted) {
            val cursorPosition = Selection.getSelectionStart(editable).coerceAtLeast(0)
            val cursorDigitIndex = countDigitsBeforeIndex(editable.toString(), cursorPosition)

            val savedFilters = editable.filters
            editable.filters = emptyArray()
            editable.replace(0, editable.length, formatted)
            editable.filters = savedFilters

            val newCursor = findIndexForDigitPosition(formatted, cursorDigitIndex)
            updateLengthFilter(editable, detected.maxLength, detected.formatGaps)

            Selection.setSelection(editable, newCursor.coerceIn(0, formatted.length))
        } else {
            updateLengthFilter(editable, detected.maxLength, detected.formatGaps)
        }

        isFormatting = false
    }

    private fun updateLengthFilter(editable: Editable, maxDigits: Int, formatGaps: IntArray) {
        val spaceCount = formatGaps.count { it < maxDigits }
        val maxLengthWithSpaces = maxDigits + spaceCount

        val existingFilters = editable.filters.filterNot { it is InputFilter.LengthFilter }
        editable.filters = (existingFilters + InputFilter.LengthFilter(maxLengthWithSpaces)).toTypedArray()
    }

    companion object {
        /** Inserts spaces into raw [digits] at the positions specified by [formatGaps]. */
        fun formatCardNumber(digits: String, formatGaps: IntArray): String {
            if (digits.isEmpty()) return ""
            val formattedCCNumber = StringBuilder(digits.length + formatGaps.size)
            for (cardDigitPosition in digits.indices) {
                if (cardDigitPosition in formatGaps) formattedCCNumber.append(' ')
                formattedCCNumber.append(digits[cardDigitPosition])
            }
            return formattedCCNumber.toString()
        }

        fun countDigitsBeforeIndex(text: String, index: Int): Int {
            return text.take(index).count { it.isDigit() }
        }

        /** Returns the character index in formatted [text] that follows the Nth digit, used for cursor placement. */
        fun findIndexForDigitPosition(text: String, digitPosition: Int): Int {
            if (digitPosition == 0) return 0
            var count = 0
            for (charIndex in text.indices) {
                if (text[charIndex].isDigit()) {
                    count++
                    if (count == digitPosition) return charIndex + 1
                }
            }
            return text.length
        }
    }
}
