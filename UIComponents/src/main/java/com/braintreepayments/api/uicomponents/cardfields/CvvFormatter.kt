package com.braintreepayments.api.uicomponents.cardfields

import android.text.Editable
import android.text.InputFilter
import android.text.Selection
import android.text.TextWatcher

internal class CvvFormatter(private var cvvLength: Int = DEFAULT_CVV_LENGTH) : TextWatcher {

    private var isFormatting = false

    fun updateCvvLength(length: Int) {
        cvvLength = length
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(editable: Editable?) {
        if (isFormatting || editable == null) return
        isFormatting = true

        val digits = editable.toString().filter { it.isDigit() }
        val truncated = digits.take(cvvLength)

        if (editable.toString() != truncated) {
            editable.replace(0, editable.length, truncated)
            Selection.setSelection(editable, truncated.length)
        }

        updateLengthFilter(editable, cvvLength)

        isFormatting = false
    }

    private fun updateLengthFilter(editable: Editable, maxLength: Int) {
        val existingFilters = editable.filters.filterNot { it is InputFilter.LengthFilter }
        editable.filters = (existingFilters + InputFilter.LengthFilter(maxLength)).toTypedArray()
    }

    companion object {
        const val DEFAULT_CVV_LENGTH = 3
    }
}