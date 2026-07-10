package com.braintreepayments.api.uicomponents.cardfields

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.braintreepayments.api.uicomponents.R

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
    /** Minimum accepted CVV length. Equals [cvvLength] for all known brands; UNKNOWN accepts 3–4. */
    val minCvvLength: Int = cvvLength,
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
        prefixPatterns = listOf("^62(?!2)\\d*".toRegex()),
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

    ELO(
        prefixPatterns = listOf(
            "^(40117[89]|438935|45763[12]|431274|451416|457393)\\d*".toRegex(),
            "^(504175|506(699|7[0-6]\\d|77[0-8])|509)\\d*".toRegex(),
            "^(627780|636297|636368)\\d*".toRegex(),
            "^(65003[1-3]|6500(3[5-9]|4\\d|5[01]))\\d*".toRegex(),
            "^(650(40[5-9]|4[1-3]\\d)|650(48[5-9]|49\\d|5[0-2]\\d|53[0-8]))\\d*".toRegex(),
            "^(650(54[1-9]|5[5-8]\\d|59[0-8])|650(70\\d|71[0-8])|65072[0-7])\\d*".toRegex(),
            "^(650(90[1-9]|9[1-6]\\d|97[0-8])|651(65[2-9]|6[6-7]\\d))\\d*".toRegex(),
            "^(6550[01]\\d|6550(2[1-9]|[34]\\d|5[0-8]))\\d*".toRegex()
        ),
        validLengths = setOf(16),
        cvvLength = 3
    ),

    MIR(
        prefixPatterns = listOf("^220[0-4]\\d*".toRegex()),
        validLengths = setOf(16, 17, 18, 19),
        cvvLength = 3
    ),

    VERVE(
        prefixPatterns = listOf(
            "^(506(099|1[01]\\d|12[0-7])|506129)\\d*".toRegex(),
            "^(506(13[3-9]|14\\d|150)|506(15[89]|16[0-3]))\\d*".toRegex(),
            "^(506166|506168|506170|506173|506(17[6-9]|180))\\d*".toRegex(),
            "^(506184|50618[78]|506191|506195|506197)\\d*".toRegex(),
            "^(50786[56]|507(86[89]|87[0-7])|50788[0-8]|507900|507941)\\d*".toRegex()
        ),
        validLengths = setOf(16, 18, 19),
        cvvLength = 3
    ),

    UNKNOWN(
        prefixPatterns = emptyList(),
        validLengths = setOf(12, 13, 14, 15, 16, 17, 18, 19),
        cvvLength = 4,
        minCvvLength = 3
    );

    val minLength: Int get() = validLengths.min()

    val maxLength: Int get() = validLengths.max()

    @get:DrawableRes
    val iconRes: Int
        get() = when (this) {
            VISA -> R.drawable.card_fields_cc_visa
            MASTERCARD -> R.drawable.card_fields_cc_mastercard
            DISCOVER -> R.drawable.card_fields_cc_discover
            AMEX -> R.drawable.card_fields_cc_amex
            JCB -> R.drawable.card_fields_cc_jcb
            UNIONPAY -> R.drawable.card_fields_cc_union_pay
            DINERS_CLUB -> R.drawable.card_fields_cc_unknown
            MAESTRO -> R.drawable.card_fields_cc_unknown
            HIPER -> R.drawable.card_fields_cc_unknown
            HIPERCARD -> R.drawable.card_fields_cc_unknown
            ELO -> R.drawable.card_fields_cc_unknown
            MIR -> R.drawable.card_fields_cc_unknown
            VERVE -> R.drawable.card_fields_cc_unknown
            UNKNOWN -> R.drawable.card_fields_cc_unknown
        }

    @get:StringRes
    val iconContentDescriptionRes: Int
        get() = when (this) {
            VISA -> R.string.card_icon_visa
            MASTERCARD -> R.string.card_icon_mastercard
            DISCOVER -> R.string.card_icon_discover
            AMEX -> R.string.card_icon_amex
            JCB -> R.string.card_icon_jcb
            UNIONPAY -> R.string.card_icon_unionpay
            DINERS_CLUB -> R.string.card_icon_diners_club
            MAESTRO -> R.string.card_icon_maestro
            HIPER -> R.string.card_icon_hiper
            HIPERCARD -> R.string.card_icon_hipercard
            ELO -> R.string.card_icon_elo
            MIR -> R.string.card_icon_mir
            VERVE -> R.string.card_icon_verve
            UNKNOWN -> R.string.card_icon_unknown
        }

    companion object {
        /**
         * Resolves a [CardBrand] from a brand name by matching the enum name case- and
         * separator-insensitively (e.g. "Visa" → [VISA], "Union Pay" → [UNIONPAY]), returning
         * [UNKNOWN] when unrecognized. Mirrors [resolveBrand] (number-based) and the SDK's
         * enum-name matching convention (see `BinType.fromString`).
         */
        fun fromDisplayName(name: String?): CardBrand {
            val normalized = name?.lowercase()?.filter { it.isLetterOrDigit() } ?: return UNKNOWN
            return entries.firstOrNull { it.name.lowercase().replace("_", "") == normalized } ?: UNKNOWN
        }

        fun resolveBrand(cardNumber: String): CardBrand {
            val matches = detect(cardNumber)
            return when {
                matches.size == 1 -> matches[0]
                else -> UNKNOWN
            }
        }

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
