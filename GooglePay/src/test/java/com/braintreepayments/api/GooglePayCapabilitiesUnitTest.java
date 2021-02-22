package com.braintreepayments.api;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wallet.Wallet;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({GoogleApiAvailability.class, Wallet.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "androidx.*"})
public class GooglePayCapabilitiesUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void isGooglePayEnabled_whenConfigurationEnabledAndApiAvailable_returnsTrue() throws JSONException {
        FragmentActivity activity = mock(FragmentActivity.class);

        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .enabled(true))
                .buildConfiguration();

        GoogleApiAvailability mockGoogleApiAvailability = mock(GoogleApiAvailability.class);
        when(mockGoogleApiAvailability.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mockGoogleApiAvailability);

        assertTrue(GooglePayCapabilities.isGooglePayEnabled(activity, configuration));
    }

    @Test
    public void isGooglePayEnabled_whenConfigurationNotEnabled_returnsFalse() throws JSONException {
        FragmentActivity activity = mock(FragmentActivity.class);

        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .enabled(false))
                .buildConfiguration();

        GoogleApiAvailability mockGoogleApiAvailability = mock(GoogleApiAvailability.class);
        when(mockGoogleApiAvailability.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mockGoogleApiAvailability);

        assertFalse(GooglePayCapabilities.isGooglePayEnabled(activity, configuration));
    }

    @Test
    public void isGooglePayEnabled_whenApiNotAvailable_returnsFalse() throws JSONException {
        FragmentActivity activity = mock(FragmentActivity.class);

        Configuration configuration = new TestConfigurationBuilder()
                .googlePay(new TestConfigurationBuilder.TestGooglePayConfigurationBuilder()
                        .enabled(true))
                .buildConfiguration();

        GoogleApiAvailability mockGoogleApiAvailability = mock(GoogleApiAvailability.class);
        when(mockGoogleApiAvailability.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.NETWORK_ERROR);

        mockStatic(GoogleApiAvailability.class);
        when(GoogleApiAvailability.getInstance()).thenReturn(mockGoogleApiAvailability);

        assertFalse(GooglePayCapabilities.isGooglePayEnabled(activity, configuration));
    }
}