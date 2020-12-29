package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.internal.AppHelper;
import com.braintreepayments.api.internal.ManifestValidator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeviceInspectorTest {

    private Context context;
    private AppHelper appHelper;
    private ManifestValidator manifestValidator;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        appHelper = mock(AppHelper.class);
        manifestValidator = mock(ManifestValidator.class);
    }

    @Test
    public void isPayPalInstalled_forwardsResultFromAppHelper() {
        when(appHelper.isAppInstalled(context, "com.paypal.android.p2pmobile")).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isPayPalInstalled(context));
    }

    @Test
    public void isVenmoInstalled_forwardsResultFromAppHelper() {
        when(appHelper.isAppInstalled(context, "com.venmo")).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.isVenmoInstalled(context));
    }

    @Test
    public void canBrowserSwitch_forwardsResultFromManifestValidator() {
        String returnUrlScheme = "sample.scheme";
        when(manifestValidator.isUrlSchemeDeclaredInAndroidManifest(context, returnUrlScheme, BraintreeBrowserSwitchActivity.class)).thenReturn(true);

        DeviceInspector sut = new DeviceInspector(appHelper, manifestValidator);
        assertTrue(sut.canBrowserSwitch(context, returnUrlScheme));
    }
}