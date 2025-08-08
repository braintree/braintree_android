package com.braintreepayments.api.sharedutils

/**
 * Exception thrown when a 403 HTTP_FORBIDDEN response is encountered. Indicates the current
 * authorization does not have permission to make the request.
 */
class AuthorizationException(message: String) : Exception(message)
