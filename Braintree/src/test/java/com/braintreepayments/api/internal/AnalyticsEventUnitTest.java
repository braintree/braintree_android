package com.braintreepayments.api.internal;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.Venmo;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class AnalyticsEventUnitTest {

    @Test
    public void createAnalyticsRequest_setsPropertiesCorrectly() throws JSONException {
        AnalyticsEvent analyticsEvent = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId",
                "custom", "card.nonce-received");

        assertEquals("android.custom.card.nonce-received", analyticsEvent.event);
        assertTrue(analyticsEvent.timestamp > 0);

        assertEquals("sessionId", analyticsEvent.metadata.getString("sessionId"));
        assertNotNull(analyticsEvent.metadata.getString("deviceNetworkType"));
        assertNotNull(analyticsEvent.metadata.getString("userInterfaceOrientation"));
        assertEquals(BuildConfig.VERSION_NAME, analyticsEvent.metadata.getString("merchantAppVersion"));
        assertEquals(PayPalOneTouchCore.isWalletAppInstalled(RuntimeEnvironment.application),
                analyticsEvent.metadata.getBoolean("paypalInstalled"));
        assertEquals(Venmo.isVenmoInstalled(RuntimeEnvironment.application),
                analyticsEvent.metadata.getBoolean("venmoInstalled"));
    }
}
