package com.braintreepayments.api;


import org.json.JSONObject;

/**
 * Communicates JSON response from a tokenization request on the main thread.
 */
interface TokenizeCallback {

    /**
     * @param tokenizationResponse JSON object created from the tokenization response.
     * @param exception error that caused the request to fail.
     */
    void onResult(JSONObject tokenizationResponse, Exception exception);
}
