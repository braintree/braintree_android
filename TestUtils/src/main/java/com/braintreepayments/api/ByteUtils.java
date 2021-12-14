package com.braintreepayments.api;

import java.nio.charset.StandardCharsets;

class ByteUtils {

    private ByteUtils() {
    }

    static byte[] toByteArray(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }
}
