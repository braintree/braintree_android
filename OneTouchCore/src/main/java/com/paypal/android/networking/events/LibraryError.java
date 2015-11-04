package com.paypal.android.networking.events;

public enum LibraryError {

    /**
     * A problem occurred on the server, and was reported back to the client.
     * (500 error)
     */
    INTERNAL_SERVER_ERROR,

    /**
     * A problem occurred communicating to the server (IOException)
     */
    SERVER_COMMUNICATION_ERROR,

    /**
     * A problem occurred parsing the server response
     */
    PARSE_RESPONSE_ERROR,

    /**
     * An internal problem
     */
    INTERNAL_ERROR,

    /**
     * This device can't talk to the servers due to TLS incompatibility.
     */
    DEVICE_OS_TOO_OLD
}
