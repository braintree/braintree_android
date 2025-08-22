package com.braintreepayments.api.sharedutils

/**
 * Exception thrown when an unrecognized error occurs while communicating with a server. This may
 * represent an IOException or an unexpected HTTP response.
 */
class UnexpectedException(message: String) : Exception(message)
