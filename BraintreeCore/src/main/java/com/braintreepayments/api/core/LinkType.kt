package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * Used to describe the link type for analytics
 * Note: This enum is exposed for internal Braintree use only. Do not use.
 * It is not covered by Semantic Versioning and may change or be removed at any time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class LinkType(val stringValue: String) {
    UNIVERSAL("universal"),
    DEEPLINK("deeplink")
}
