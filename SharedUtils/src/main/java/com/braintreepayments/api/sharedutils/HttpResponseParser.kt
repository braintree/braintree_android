package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import java.net.HttpURLConnection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface HttpResponseParser {
    @Throws(Exception::class)
    fun parse(responseCode: Int, connection: HttpURLConnection): String
}
