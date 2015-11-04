package com.paypal.android.sdk.onetouch.core;

class ResponseParsingException extends Exception {
    public ResponseParsingException(String detailMessage) {
        super(detailMessage);
    }

    public ResponseParsingException(Throwable throwable) {
        super(throwable);
    }
}
