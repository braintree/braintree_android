package com.braintreepayments.api

import java.lang.Exception

/**
 * Error thrown when arguments provided to a method are invalid.
 * @param message the error message
 */
class InvalidArgumentException internal constructor(message: String?) : Exception(message)