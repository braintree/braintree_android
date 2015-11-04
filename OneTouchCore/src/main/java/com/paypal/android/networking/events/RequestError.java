package com.paypal.android.networking.events;

public class RequestError extends ErrorBase {
    /**
     * @param code an error code
     * @param shortMsg a short message
     * @param longMsg a long message
     */
    public RequestError(String code, String shortMsg, String longMsg) {
        super("RequestError", code, shortMsg, longMsg);
    }

    /**
     * @param code an error code
     */
    public RequestError(String code) {
        super("RequestError", code);
    }
}
