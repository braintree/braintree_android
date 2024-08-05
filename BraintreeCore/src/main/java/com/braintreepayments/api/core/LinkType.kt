package com.braintreepayments.api.core

/**
 * Used to describe the link type for analytics
 * Note: This enum is exposed for internal Braintree use only. Do not use.
 * It is not covered by Semantic Versioning and may change or be removed at any time.
 */
public enum class LinkType(val stringValue: String)  {
    UNIVERSAL("universal"),
    DEEPLINK("deeplink")
}