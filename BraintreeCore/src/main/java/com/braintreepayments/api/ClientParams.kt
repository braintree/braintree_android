package com.braintreepayments.api

import android.content.Context

data class ClientParams(val context: Context,
                        val authorization: String,
                        val returnUrlScheme: String?) {

    constructor(context: Context, authorization: String) : this(context, authorization, null)
}
