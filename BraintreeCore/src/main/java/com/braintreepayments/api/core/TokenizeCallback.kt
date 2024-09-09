package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import org.json.JSONObject

/**
 * Communicates JSON response from a tokenization request on the main thread.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface TokenizeCallback {

    /**
     * @param tokenizationResponse JSON object created from the tokenization response.
     * @param exception error that caused the request to fail.
     */
    fun onResult(tokenizationResponse: JSONObject?, exception: Exception?)
}
