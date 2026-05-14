package com.braintreepayments.api.uicomponents.cardfields

import android.text.Editable
import android.text.InputFilter
import android.text.Selection
import android.text.TextWatcher

@Suppress("MagicNumber")
internal class ExpirationDateFormatter : TextWatcher {

    private var isFormatting = false
    private var lastActionWasDelete = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        lastActionWasDelete = before > count
    }

    override fun afterTextChanged(editable: Editable?) {
        if (isFormatting || editable == null) return
        isFormatting = true

        val cursorPosition = Selection.getSelectionStart(editable).coerceAtLeast(0)
        var cursorDigitIndex = editable.toString().take(cursorPosition).count { it.isDigit() }

        var digits = editable.toString().filter { it.isDigit() }.take(MAX_DIGITS)

        if (!lastActionWasDelete && applyLeadingZero(digits) != digits) {
            digits = applyLeadingZero(digits)
            cursorDigitIndex += 1
        }

        if (!isValidMonth(digits)) {
            digits = digits.take(1)
            cursorDigitIndex = minOf(cursorDigitIndex, 1)
        }

        val formatted = formatExpiration(digits)

        if (editable.toString() != formatted) {
            val savedFilters = editable.filters
            editable.filters = emptyArray()
            editable.replace(0, editable.length, formatted)
            editable.filters = savedFilters

            val newCursor = findCursorPosition(formatted, cursorDigitIndex)
            editable.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
            Selection.setSelection(editable, newCursor.coerceIn(0, formatted.length))
        } else {
            editable.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        }

        isFormatting = false
    }

    companion object {
        const val MAX_DIGITS = 4
        const val MAX_LENGTH = 5 // MM/YY

        fun applyLeadingZero(digits: String): String =
            if (digits.length == 1 && digits[0] > '1') "0$digits" else digits

        fun isValidMonth(digits: String): Boolean {
            if (digits.length < 2) return true
            val month = digits.take(2).toIntOrNull() ?: return false
            return month in 1..12
        }

        fun formatExpiration(digits: String): String = when {
            digits.length <= 2 -> digits
            else -> "${digits.take(2)}/${digits.drop(2)}"
        }

        fun findCursorPosition(formatted: String, digitIndex: Int): Int {
            if (digitIndex == 0) return 0
            var count = 0
            for (i in formatted.indices) {
                if (formatted[i].isDigit()) {
                    count++
                    if (count == digitIndex) return i + 1
                }
            }
            return formatted.length
        }
    }
}
