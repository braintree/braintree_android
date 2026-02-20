package com.braintreepayments.api.testutils

import android.util.Base64

object FixturesHelper {

    fun base64Encode(value: String): String {
        return Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
    }
}
