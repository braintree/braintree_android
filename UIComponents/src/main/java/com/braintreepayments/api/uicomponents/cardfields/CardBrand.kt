package com.braintreepayments.api.uicomponents.cardfields

@Suppress("MagicNumber")
internal enum class CardBrand(
    /** Strict prefix patterns checked first across all brands before relaxed patterns. */
    val prefixPatterns: List<Regex>,
    /**
     * Relaxed prefix patterns only used if no strict match is found across all brands.
     * Only Maestro uses relaxed matching — mirrors the card-form's/drop-in's two-pass detection.
     */
    val relaxedPrefixPatterns: List<Regex> = emptyList(),
    val validLengths: Set<Int>,
    val cvvLength: Int,
    val formatGaps: IntArray = intArrayOf(4, 8, 12)
) {
    VISA(
        prefixPatterns = listOf("^4\\d*".toRegex()),
        validLengths = setOf(16),
        cvvLength = 3
    ),

    MASTERCARD(
        prefixPatterns = listOf("^(5[1-5]|222[1-9]|22[3-9]|2[3-6]|27[0-1]|2720)\\d*".toRegex()),
        validLengths = setOf(16),
        cvvLength = 3
    ),

    DISCOVER(
        prefixPatterns = listOf("^(6011|65|64[4-9]|622)\\d*".toRegex()),
        validLengths = setOf(16, 17, 18, 19),
        cvvLength = 3
    ),

    AMEX(
        prefixPatterns = listOf("^3[47]\\d*".toRegex()),
        validLengths = setOf(15),
        cvvLength = 4,
        formatGaps = intArrayOf(4, 10)
    ),

    DINERS_CLUB(
        prefixPatterns = listOf("^(36|38|30[0-5])\\d*".toRegex()),
        validLengths = setOf(14),
        cvvLength = 3,
        formatGaps = intArrayOf(4, 10)
    ),

    JCB(
        prefixPatterns = listOf("^35\\d*".toRegex()),
        validLengths = setOf(16),
        cvvLength = 3
    ),

    MAESTRO(
        prefixPatterns = listOf(
            "^(5018|5020|5038|5[6-9]|6020|6304|6703|6759|676[1-3])\\d*".toRegex()
        ),
        relaxedPrefixPatterns = listOf("^6\\d*".toRegex()),
        validLengths = setOf(12, 13, 14, 15, 16, 17, 18, 19),
        cvvLength = 3
    ),

    UNIONPAY(
        prefixPatterns = listOf("^62\\d*".toRegex()),
        validLengths = setOf(16, 17, 18, 19),
        cvvLength = 3
    ),

    HIPER(
        prefixPatterns = listOf("^637(095|568|599|609|612)\\d*".toRegex()),
        validLengths = setOf(16),
        cvvLength = 3
    ),

    HIPERCARD(
        prefixPatterns = listOf("^606282\\d*".toRegex()),
        validLengths = setOf(16),
        cvvLength = 3
    ),

    UNKNOWN(
        prefixPatterns = emptyList(),
        validLengths = setOf(12, 13, 14, 15, 16, 17, 18, 19),
        cvvLength = 3
    );

    val minLength: Int get() = validLengths.min()

    val maxLength: Int get() = validLengths.max()

    companion object {
        /**
         * Detects which card brands could match the given card number.
         *
         * Returns a list because early digits can be ambiguous.
         * The caller will show a generic icon when the list has multiple matches
         * and switch to a brand-specific icon once it narrows to one.
         *
         * When the input is empty, returns an empty list.
         *
         * @param cardNumber Raw input string (spaces are filtered out internally).
         * @return List of matching brands, or an empty list if input is empty or digits are present
         * but no brand matches (caller should treat this as [UNKNOWN]).
         */
        fun detect(cardNumber: String): List<CardBrand> {
            val digits = cardNumber.filter { it.isDigit() }
            if (digits.isEmpty()) return emptyList()

            val strictMatches = entries.filter { brand ->
                brand.prefixPatterns.any { it.containsMatchIn(digits) }
            }
            if (strictMatches.isNotEmpty()) return strictMatches

            return entries.filter { brand ->
                brand.relaxedPrefixPatterns.any { it.containsMatchIn(digits) }
            }
        }
    }
}
