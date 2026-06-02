package com.braintreepayments.api.uicomponents.cardfields

import androidx.annotation.StringRes

internal sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(@StringRes val errorMessageRes: Int) : ValidationResult()
    data object Validating : ValidationResult()
}