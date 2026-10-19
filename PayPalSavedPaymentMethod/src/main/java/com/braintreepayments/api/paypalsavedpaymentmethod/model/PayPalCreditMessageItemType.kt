package com.braintreepayments.api.paypalsavedpaymentmethod.model

/**
 * The kind of content block making up a presentment message.
 */
internal enum class PayPalCreditMessageItemType(val rawValue: String) {

    /**
     * A logo image, with [PayPalCreditMessageItem.alternativeText] as its alt text.
     */
    IMAGE("IMAGE"),

    /**
     * Tappable copy that opens [PayPalCreditMessageItem.clickUrl], such as "Learn more".
     */
    LINK("LINK"),

    /**
     * Plain copy.
     */
    TEXT("TEXT");

    companion object {

        /**
         * Returns `null` when PayPal returns a type this SDK version does not recognize.
         */
        fun fromRawValue(rawValue: String?): PayPalCreditMessageItemType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}
