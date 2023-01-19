package com.braintreepayments.api

internal class InvalidAuthorization(rawValue: String?, val errorMessage: String) :
    Authorization(rawValue) {
    
    public override fun getConfigUrl(): String? {
        return null
    }

    public override fun getBearer(): String? {
        return null
    }
}