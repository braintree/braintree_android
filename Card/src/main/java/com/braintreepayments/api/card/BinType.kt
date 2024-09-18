package com.braintreepayments.api.card

enum class BinType {
    Yes,
    No,
    Unknown;

    companion object {
        internal fun fromString(string: String): BinType {
            return when (string.lowercase()) {
                Yes.name.lowercase() -> Yes
                No.name.lowercase() -> No
                else -> Unknown
            }
        }
    }
}
