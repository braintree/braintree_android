package com.paypal.android.sdk.onetouch.core.config;

import android.content.Context;
import android.content.Intent;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.AppHelper;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;
import com.paypal.android.sdk.onetouch.core.sdk.AppSwitchHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AppHelper.class, AppSwitchHelper.class })
public class ConfigManagerUnitTest {

    private static final String CONFIGURATION_URL = "https://www.paypalobjects.com/webstatic/otc/otc-config.android.json";

    private ContextInspector mContextInspector;
    private PayPalHttpClient mHttpClient;
    private ConfigManager mConfigManager;

    @Before
    public void setup() {
        mContextInspector = mock(ContextInspector.class);
        mHttpClient = mock(PayPalHttpClient.class);
        mConfigManager = new ConfigManager(mContextInspector, mHttpClient);
    }

    @Test
    public void refreshConfiguration_makesGetRequestForConfiguration() {
        mConfigManager.refreshConfiguration();

        verify(mHttpClient).get(eq(CONFIGURATION_URL), any(HttpResponseCallback.class));
    }

    @Test
    public void refreshConfiguration_doesNotRequestConfigurationWhenUseHardCodedConfigEnabled() {
        mConfigManager.useHardcodedConfig(true);
        mConfigManager.refreshConfiguration();

        verifyZeroInteractions(mHttpClient);
    }

    @Test
    public void refreshConfiguration_makesOneRequestWhenCalledRepeatedly() {
        for (int i = 0; i < 10; i++) {
            mConfigManager.refreshConfiguration();
        }

        verify(mHttpClient, times(1)).get(eq(CONFIGURATION_URL), any(HttpResponseCallback.class));
    }

    @Test
    public void getConfig_callsRefreshConfiguration() {
        mConfigManager.getConfig();

        verify(mHttpClient).get(eq(CONFIGURATION_URL), any(HttpResponseCallback.class));
    }

    @Test
    public void getConfig_returnsConfig() {
        OtcConfiguration configuration = mConfigManager.getConfig();

        assertEquals("2016-03-10T21:15:00Z", configuration.getFileTimestamp());
        assertEquals(2, configuration.getBillingAgreementRecipes().size());
        assertEquals(RequestTarget.wallet, configuration.getBillingAgreementRecipes().get(0).getTarget());
        assertEquals(RequestTarget.browser, configuration.getBillingAgreementRecipes().get(1).getTarget());
        assertEquals(4, configuration.getCheckoutRecipes().size());
        assertEquals(RequestTarget.wallet, configuration.getCheckoutRecipes().get(0).getTarget());
        assertEquals(RequestTarget.wallet, configuration.getCheckoutRecipes().get(1).getTarget());
        assertEquals(RequestTarget.wallet, configuration.getCheckoutRecipes().get(2).getTarget());
        assertEquals(RequestTarget.browser, configuration.getCheckoutRecipes().get(3).getTarget());
        assertNotNull(configuration.getBrowserBillingAgreementConfig());
        assertNotNull(configuration.getBrowserCheckoutConfig());
        assertNotNull(configuration.getOauth2Recipes());
    }

    @Test
    public void isValidAppTarget_returnsConfig_validPackageName() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);

        when(appContext.getPackageName()).thenReturn("com.example.app");
        mockStatic(AppHelper.class);
        mockStatic(AppSwitchHelper.class);

        when(AppSwitchHelper.createBaseIntent(any(String.class), any(String.class))).thenReturn(mock(Intent.class));
        when(AppSwitchHelper.isSignatureValid(any(Context.class), any(String.class))).thenReturn(true);
        when(AppHelper.isIntentAvailable(any(Context.class), any(Intent.class))).thenReturn(true);

        OtcConfiguration configuration = mConfigManager.getConfig();

        for (Recipe r: configuration.getBillingAgreementRecipes()) {
            assertTrue(r.isValidAppTarget(context));
        }

        for (Recipe r: configuration.getCheckoutRecipes()) {
            assertTrue(r.isValidAppTarget(context));
        }
    }

    @Test
    public void isValidAppTarget_returnsConfig_invalidPackageName() {
        Context context = mock(Context.class);
        Context appContext = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(appContext);

        when(appContext.getPackageName()).thenReturn("com.example.App");

        OtcConfiguration configuration = mConfigManager.getConfig();

        for (Recipe r: configuration.getBillingAgreementRecipes()) {
            assertFalse(r.isValidAppTarget(context));
        }

        for (Recipe r: configuration.getCheckoutRecipes()) {
            assertFalse(r.isValidAppTarget(context));
        }
    }
}
