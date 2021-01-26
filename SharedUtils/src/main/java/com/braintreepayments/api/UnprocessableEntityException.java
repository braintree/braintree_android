package com.braintreepayments.api;

/**
 * Exception thrown when a 422 HTTP_UNPROCESSABLE_ENTITY response is encountered. Indicates the
 * request was invalid in some way.
 */
public class UnprocessableEntityException extends Exception {

    public UnprocessableEntityException(String message) {
        super(message);
    }
}
