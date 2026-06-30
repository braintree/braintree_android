package com.braintreepayments.api.uicomponents.cardfields

@Suppress("MagicNumber")
internal object LuhnValidator {

    /**
     * Performs the Luhn check on the given card number.
     *
     * @param cardNumber a String consisting of numeric digits (only).
     * @return `true` if the sequence passes the checksum.
     * @throws IllegalArgumentException if [cardNumber] contains a non-digit character.
     * @see <a href="https://en.wikipedia.org/wiki/Luhn_algorithm">Luhn Algorithm (Wikipedia)</a>
     */
    fun isLuhnValid(cardNumber: String): Boolean {
        val reversed = cardNumber.reversed()
        var oddSum = 0
        var evenSum = 0
        for (i in reversed.indices) {
            val digit = reversed[i].digitToIntOrNull()
                ?: throw IllegalArgumentException("Not a digit: '${reversed[i]}'")
            if (i % 2 == 0) {
                oddSum += digit
            } else {
                evenSum += digit / 5 + (2 * digit) % 10
            }
        }
        return (oddSum + evenSum) % 10 == 0
    }
}
