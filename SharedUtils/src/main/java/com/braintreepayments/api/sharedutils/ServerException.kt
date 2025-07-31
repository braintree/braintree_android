package com.braintreepayments.api.sharedutils

/**
 * Exception thrown when a 500 HTTP_INTERNAL_ERROR response is encountered. Indicates an unexpected
 * error from the server.
 */
class ServerException(message: String) : Exception(message)
