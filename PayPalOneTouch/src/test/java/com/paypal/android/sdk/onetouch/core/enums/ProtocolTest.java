package com.paypal.android.sdk.onetouch.core.enums;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ProtocolTest {

    @Test
    public void v0_returnsCorrectVersion() {
        assertEquals("0.0", Protocol.v0.getVersion());
    }

    @Test
    public void v1_returnsCorrectVersion() {
        assertEquals("1.0", Protocol.v1.getVersion());
    }

    @Test
    public void v2_returnsCorrectVersion() {
        assertEquals("2.0", Protocol.v2.getVersion());
    }

    @Test
    public void v3_returnsCorrectVersion() {
        assertEquals("3.0", Protocol.v3.getVersion());
    }
}
