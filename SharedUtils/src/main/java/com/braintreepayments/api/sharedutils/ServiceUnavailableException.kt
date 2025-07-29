package com.braintreepayments.api.sharedutils

/**
 * Exception thrown when a 503 HTTP_UNAVAILABLE response is encountered. Indicates the server is
 * unreachable or the request timed out.
 */
class ServiceUnavailableException(message: String) : Exception(message)
