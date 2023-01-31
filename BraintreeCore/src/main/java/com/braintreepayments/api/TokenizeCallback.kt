package com.braintreepayments.api

import androidx.annotation.RestrictTo
import org.json.JSONObject
import java.lang.Exception

/**
 * Communicates JSON response from a tokenization request on the main thread.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface TokenizeCallback {

    /**
     * @param tokenizationResponse JSON object created from the tokenization response.
     * @param exception error that caused the request to fail.
     */
    fun onResult(tokenizationResponse: JSONObject?, exception: Exception?)
}
