package com.braintreepayments.api.core

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MerchantRepository {

    lateinit var applicationContext: Context
    lateinit var integrationType: IntegrationType
    lateinit var authorization: Authorization
    lateinit var returnUrlScheme: String
    var appLinkReturnUri: Uri? = null

    companion object {
        val instance: MerchantRepository by lazy { MerchantRepository() }
    }
}
