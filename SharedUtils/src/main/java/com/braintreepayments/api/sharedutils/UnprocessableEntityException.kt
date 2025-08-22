package com.braintreepayments.api.sharedutils

/**
 * Exception thrown when a 422 HTTP_UNPROCESSABLE_ENTITY response is encountered. Indicates the
 * request was invalid in some way.
 */
class UnprocessableEntityException(message: String) : Exception(message)
