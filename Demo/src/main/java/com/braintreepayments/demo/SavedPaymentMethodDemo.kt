package com.braintreepayments.demo

/** Values and input checks shared by the saved payment method screens and the vault helper. */
internal object SavedPaymentMethodDemo {

    const val APP_LINK_RETURN_URL = "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
    const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"
    const val DEFAULT_AMOUNT = "10.00"

    // The Demo merchant's PayPal account is French, so PayPal returns no Pay Later message for USD.
    const val DEFAULT_CURRENCY = "EUR"

    // ISO 4217 currency codes have three letters.
    private const val CURRENCY_CODE_LENGTH = 3

    // Plain digits with at most two decimals, the format PayPal accepts; no exponent or sign.
    private val AMOUNT_PATTERN = Regex("""\d+(\.\d{1,2})?""")

    /** Empty falls back to [DEFAULT_AMOUNT]; anything but a positive amount with up to 2 decimals is rejected. */
    fun validAmountOrNull(text: String): String? =
        text.trim().ifEmpty { DEFAULT_AMOUNT }
            .takeIf { AMOUNT_PATTERN.matches(it) && it.toBigDecimal().signum() == 1 }

    /** Empty falls back to [DEFAULT_CURRENCY]; anything that isn't three letters A to Z is rejected. */
    fun validCurrencyOrNull(text: String): String? =
        text.trim().uppercase().ifEmpty { DEFAULT_CURRENCY }
            .takeIf { it.length == CURRENCY_CODE_LENGTH && it.all { char -> char in 'A'..'Z' } }
}
