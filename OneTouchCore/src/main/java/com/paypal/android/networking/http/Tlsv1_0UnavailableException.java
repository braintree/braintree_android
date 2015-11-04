package com.paypal.android.networking.http;

import java.io.IOException;

public class Tlsv1_0UnavailableException extends IOException {
    public Tlsv1_0UnavailableException(IllegalArgumentException e) {
        super("TLSv1 unavailable!", e);
    }
}
