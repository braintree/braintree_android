package com.braintreepayments.api.sharedutils

/**
 * Exception thrown when a 401 HTTP_UNAUTHORIZED response is encountered. Indicates authentication
 * has failed in some way.
 */
class AuthenticationException(message: String) : Exception(message)
