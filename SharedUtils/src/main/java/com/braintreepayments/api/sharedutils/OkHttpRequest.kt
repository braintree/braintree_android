package com.braintreepayments.api.sharedutils

import java.net.URL

/**
 * NOTE THIS CLASS WILL BE RENAMED TO `HttpRequest` TO REPLACE THE EXISTING [HttpRequest] CLASS.
 *
 * Represents an HTTP request to be executed.
 *
 * This data class encapsulates the request URL, HTTP method, and headers.
 *
 * @property url The URL to which the request will be sent.
 * @property method The HTTP method to use for the request (GET, POST, etc.).
 * @property headers A map of header key-value pairs to include in the request.
 */
internal data class OkHttpRequest(
    val url: URL,
    val method: Method,
    val headers: Map<String, String> = emptyMap(),
)
