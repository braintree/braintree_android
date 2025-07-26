package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class IntegrationType(val stringValue: String) {
    CUSTOM("custom"),
    DROP_IN("dropin");

    companion object {
        internal fun fromString(stringValue: String?): IntegrationType? {
            return IntegrationType.entries.firstOrNull { it.stringValue == stringValue }
        }
    }
}
