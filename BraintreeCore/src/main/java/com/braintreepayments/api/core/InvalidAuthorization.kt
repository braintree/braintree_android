package com.braintreepayments.api.core

internal class InvalidAuthorization(
    rawValue: String, val errorMessage: String
) : Authorization(rawValue) {

    override val configUrl: String? = null
    override val bearer: String? = null
}
