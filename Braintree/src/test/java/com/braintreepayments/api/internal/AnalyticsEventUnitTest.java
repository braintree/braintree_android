package com.braintreepayments.api.internal;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.Venmo;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest({ ClassHelper.class })
public class AnalyticsEventUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void createAnalyticsRequest_setsPropertiesCorrectly() throws JSONException {
        AnalyticsEvent analyticsEvent = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId",
                "custom", "card.nonce-received");

        assertEquals("android.card.nonce-received", analyticsEvent.event);
        assertTrue(analyticsEvent.timestamp > 0);

        assertEquals("sessionId", analyticsEvent.metadata.getString("sessionId"));
        assertNotNull(analyticsEvent.metadata.getString("deviceNetworkType"));
        assertNotNull(analyticsEvent.metadata.getString("userInterfaceOrientation"));
        assertEquals(BuildConfig.VERSION_NAME, analyticsEvent.metadata.getString("merchantAppVersion"));
        assertEquals(PayPalOneTouchCore.isWalletAppInstalled(RuntimeEnvironment.application),
                analyticsEvent.metadata.getBoolean("paypalInstalled"));
        assertEquals(Venmo.isVenmoInstalled(RuntimeEnvironment.application),
                analyticsEvent.metadata.getBoolean("venmoInstalled"));
        assertEquals("custom",
                analyticsEvent.metadata.getString("integrationType"));
        assertFalse(analyticsEvent.metadata.has("dropinVersion"));
    }

    @Test
    public void createAnalyticsRequest_whenDropInAvailable_setsdropinVersion() throws JSONException {
        mockStatic(ClassHelper.class);
        doReturn("expected-drop-in-version").when(ClassHelper.class);
        ClassHelper.getFieldValue(
                eq("com.braintreepayments.api.dropin.BuildConfig"),
                eq("VERSION_NAME")
        );

        AnalyticsEvent analyticsEvent = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId",
                "custom", "card.nonce-received");
        assertEquals("expected-drop-in-version", analyticsEvent.metadata.getString("dropinVersion"));
    }
}
