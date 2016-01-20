package com.paypal.android.sdk.onetouch.core.config;

import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.enums.RequestTarget;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricGradleTestRunner.class)
public class ConfigManagerTest {

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

        assertEquals("2015-12-08T00:03:56Z", configuration.getFileTimestamp());
        assertEquals(2, configuration.getBillingAgreementRecipes().size());
        assertEquals(RequestTarget.wallet, configuration.getBillingAgreementRecipes().get(0).getTarget());
        assertEquals(RequestTarget.browser, configuration.getBillingAgreementRecipes().get(1).getTarget());
        assertEquals(3, configuration.getCheckoutRecipes().size());
        assertEquals(RequestTarget.wallet, configuration.getCheckoutRecipes().get(0).getTarget());
        assertEquals(RequestTarget.wallet, configuration.getCheckoutRecipes().get(1).getTarget());
        assertEquals(RequestTarget.browser, configuration.getCheckoutRecipes().get(2).getTarget());
        assertNotNull(configuration.getBrowserBillingAgreementConfig());
        assertNotNull(configuration.getBrowserCheckoutConfig());
        assertNotNull(configuration.getOauth2Recipes());
    }
}
