package com.paypal.android.networking.events;

public class ThrowableEvent extends ErrorBase {

    public ThrowableEvent(String code, Throwable e) {
        super(e.getClass().toString(), code, e.toString(), e.getMessage());
    }

    public ThrowableEvent(LibraryError libraryError, Exception e) {
        this(libraryError.toString(), e);
    }
}
