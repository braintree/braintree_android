package com.braintreepayments.api.sharedutils

/**
 * Represents an HTTP method for network requests.
 *
 * This sealed class provides types for supported HTTP methods, such as GET and POST.
 *
 * @property stringValue The string representation of the HTTP method (e.g., "GET", "POST").
 */
internal sealed class Method(val stringValue: String) {
    /**
     * Represents the HTTP GET method.
     */
    object Get : Method("GET")

    /**
     * Represents the HTTP POST method.
     *
     * @property body The request body to send with the POST request.
     */
    class Post(val body: String) : Method("POST")
}
