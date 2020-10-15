package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.internal.AppHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AppHelper.class })
public class DeviceCapabilitiesTest {

    private Context context;

    @Before
    public void beforeEach() {
        mockStatic(AppHelper.class);

        context = mock(Context.class);
    }

    @Test
    public void isPayPalInstalled_forwardsResultFromAppHelper() {
        when(AppHelper.isAppInstalled(context, "com.paypal.android.p2pmobile")).thenReturn(true);

        assertTrue(DeviceCapabilities.isPayPalInstalled(context));
    }

    @Test
    public void isVenmoInstalled_forwardsResultFromAppHelper() {
        when(AppHelper.isAppInstalled(context, "com.venmo")).thenReturn(true);

        assertTrue(DeviceCapabilities.isVenmoInstalled(context));
    }
}