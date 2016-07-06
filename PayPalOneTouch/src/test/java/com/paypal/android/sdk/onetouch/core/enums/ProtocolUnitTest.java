package com.paypal.android.sdk.onetouch.core.enums;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ProtocolUnitTest {

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

    @Test
    public void getProtocol_returnsCorrectVersionFor0() {
        assertEquals(Protocol.v0, Protocol.getProtocol("0"));
    }

    @Test
    public void getProtocol_returnsCorrectVersionFor1() {
        assertEquals(Protocol.v1, Protocol.getProtocol("1"));
    }

    @Test
    public void getProtocol_returnsCorrectVersionFor2() {
        assertEquals(Protocol.v2, Protocol.getProtocol("2"));
    }

    @Test
    public void getProtocol_returnsCorrectVersionFor3() {
        assertEquals(Protocol.v3, Protocol.getProtocol("3"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getProtocol_throwsExceptionForUnknownVersion() {
        Protocol.getProtocol("4");
    }
}
