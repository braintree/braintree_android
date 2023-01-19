package com.braintreepayments.api

internal class InvalidAuthorization(rawValue: String, val errorMessage: String) :
    Authorization(rawValue) {


    //region Authorization overrides
    override val configUrl: String? = null

    override val bearer: String? = null
    //endregion
}