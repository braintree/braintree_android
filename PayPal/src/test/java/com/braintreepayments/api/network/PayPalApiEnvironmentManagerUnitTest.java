package com.braintreepayments.api.network;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class PayPalApiEnvironmentManagerUnitTest {

    @Test
    public void isMock_returnsTrueForMockEnvironment() {
        assertTrue(PayPalApiEnvironmentManager.isMock("mock"));
    }

    @Test
    public void isMock_returnsFalseForNonMockString() {
        assertFalse(PayPalApiEnvironmentManager.isMock("sandbox"));
    }

    @Test
    public void isSandbox_returnsTrueForSandboxEnvironment() {
        assertTrue(PayPalApiEnvironmentManager.isSandbox("sandbox"));
    }

    @Test
    public void isSandbox_returnsFalseForNonSandboxString() {
        assertFalse(PayPalApiEnvironmentManager.isSandbox("live"));
    }

    @Test
    public void isLive_returnsTrueForLiveEnvironment() {
        assertTrue(PayPalApiEnvironmentManager.isLive("live"));
    }

    @Test
    public void isLive_returnsFalseForNonLiveString() {
        assertFalse(PayPalApiEnvironmentManager.isLive("mock"));
    }

    @Test
    public void isStage_returnsTrueForNonMockSandboxOrLiveEnvironment() {
        assertTrue(PayPalApiEnvironmentManager.isStage("http://stage.com"));
    }

    @Test
    public void isStage_returnsFalseForMockEnvironment() {
        assertFalse(PayPalApiEnvironmentManager.isStage("mock"));
    }

    @Test
    public void isStage_returnsFalseForSandboxEnvironment() {
        assertFalse(PayPalApiEnvironmentManager.isStage("sandbox"));
    }

    @Test
    public void isStage_returnsFalseForLiveEnvironment() {
        assertFalse(PayPalApiEnvironmentManager.isStage("live"));
    }

    @Test
    public void getEnvironmentUrl_returnsNullForMock() {
        assertNull(PayPalApiEnvironmentManager.getEnvironmentUrl("mock"));
    }

    @Test
    public void getEnvironmentUrl_returnsUrlForSandbox() {
        assertEquals("https://api-m.sandbox.paypal.com/v1/",
                PayPalApiEnvironmentManager.getEnvironmentUrl("sandbox"));
    }

    @Test
    public void getEnvironmentUrl_returnsUrlForLive() {
        assertEquals("https://api-m.paypal.com/v1/",
                PayPalApiEnvironmentManager.getEnvironmentUrl("live"));
    }

    @Test
    public void getEnvironmentUrl_returnsStringForUnknownEnvironment() {
        assertEquals("http://stage.com", PayPalApiEnvironmentManager.getEnvironmentUrl("http://stage.com"));
    }
}
