package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.braintreepayments.api.internal.ClassHelper;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.powermock.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest({ ClassHelper.class })
public class AnalyticsEventUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void beforeEach() {
        mockStatic(ClassHelper.class);
    }

    @Test
    public void createAnalyticsRequest_setsPropertiesCorrectly() throws Exception {

        // WORKAROUND: Stubbing version name here because Android Gradle Plugin 4.0 causes Robolectric
        // 4.3 to return null for version name. Version name is correctly returned in the emulator
        Context context = spy(RuntimeEnvironment.application);
        PackageManager packageManager = mock(PackageManager.class);
        DeviceInspector deviceInspector = mock(DeviceInspector.class);

        when(context.getPackageManager()).thenReturn(packageManager);
        when(deviceInspector.isPayPalInstalled(context)).thenReturn(true);
        when(deviceInspector.isVenmoInstalled(context)).thenReturn(true);

        PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.versionName = "sampleVersionName";
        when(packageManager.getPackageInfo("com.braintreepayments.api.test", 0)).thenReturn(packageInfo);

        ApplicationInfo paypalAppInfo = mock(ApplicationInfo.class);
        when(packageManager.getApplicationInfo("com.paypal.android.p2pmobile", 0)).thenReturn(paypalAppInfo);

        AnalyticsEvent analyticsEvent = new AnalyticsEvent(context, "sessionId", "custom", "card.nonce-received", deviceInspector);

        assertEquals("android.card.nonce-received", analyticsEvent.event);
        assertTrue(analyticsEvent.timestamp > 0);

        assertEquals("sessionId", analyticsEvent.metadata.getString("sessionId"));
        assertNotNull(analyticsEvent.metadata.getString("deviceNetworkType"));
        assertNotNull(analyticsEvent.metadata.getString("userInterfaceOrientation"));
        assertEquals("sampleVersionName", analyticsEvent.metadata.getString("merchantAppVersion"));
        assertTrue(analyticsEvent.metadata.getBoolean("paypalInstalled"));
        assertTrue(analyticsEvent.metadata.getBoolean("venmoInstalled"));
        assertEquals("custom",
                analyticsEvent.metadata.getString("integrationType"));
        assertFalse(analyticsEvent.metadata.has("dropinVersion"));
    }

    @Test
    public void createAnalyticsRequest_whenDropInAvailable_setsdropinVersion() throws JSONException {
        doReturn("expected-drop-in-version").when(ClassHelper.class);
        ClassHelper.getFieldValue(
                eq("com.braintreepayments.api.dropin.BuildConfig"),
                eq("VERSION_NAME")
        );

        Context context = RuntimeEnvironment.application;
        AnalyticsEvent analyticsEvent = new AnalyticsEvent(context, "sessionId", "custom", "card.nonce-received");
        assertEquals("expected-drop-in-version", analyticsEvent.metadata.getString("dropinVersion"));
    }
}
