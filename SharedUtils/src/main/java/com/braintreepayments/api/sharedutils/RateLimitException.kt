package com.braintreepayments.api.sharedutils

/**
 * Exception thrown when a 429 HTTP_TOO_MANY_REQUESTS response is encountered. Indicates the client has hit a request
 * limit and should wait a period of time and try again.
 */
class RateLimitException(message: String) : Exception(message)
