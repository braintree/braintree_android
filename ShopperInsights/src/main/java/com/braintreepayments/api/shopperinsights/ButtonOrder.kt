package com.braintreepayments.api.shopperinsights

/**
 * The order or ranking in which payment buttons appear.
 */
enum class ButtonOrder(internal val stringValue: String) {

    /**
     * First place
     */
    FIRST("1"),

    /**
     * Second place
     */
    SECOND("2"),

    /**
     * Third place
     */
    THIRD("3"),

    /**
     * Fourth place
     */
    FOURTH("4"),

    /**
     * Fifth place
     */
    FIFTH("5"),

    /**
     * Sixth place
     */
    SIXTH("6"),

    /**
     * Seventh place
     */
    SEVENTH("7"),

    /**
     * Eighth place
     */
    Eighth("8"),

    /**
     * Greater than Eighth place
     */
    OTHER("other");

    fun getStringRepresentation(): String {
        return stringValue
    }
}
