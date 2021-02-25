package com.braintreepayments.api;

/**
 * Exception thrown when a 503 HTTP_UNAVAILABLE response is encountered. Indicates the server is
 * unreachable or the request timed out.
 */
public class DownForMaintenanceException extends Exception {

    DownForMaintenanceException(String message) {
        super(message);
    }
}
