package com.braintreepayments.api.threedsecure;

import android.os.Build.VERSION_CODES;
import android.support.annotation.RequiresApi;
import android.webkit.CookieManager;
import android.webkit.WebSettings;

import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.shadows.BraintreeShadowCookieManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(sdk = 21, shadows = BraintreeShadowCookieManager.class)
@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureWebViewUnitTest {

    private ThreeDSecureWebView mThreeDSecureWebView;
    private ThreeDSecureWebViewActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mock(ThreeDSecureWebViewActivity.class);
        when(mActivity.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        mThreeDSecureWebView = new ThreeDSecureWebView(RuntimeEnvironment.application);
    }

    @Test
    public void init_setsId() {
        assertEquals(-1, mThreeDSecureWebView.getId());

        mThreeDSecureWebView.init(mActivity);

        assertEquals(android.R.id.widget_frame, mThreeDSecureWebView.getId());
    }

    @Test
    public void init_setsWebSettings() {
        mThreeDSecureWebView.init(mActivity);

        WebSettings settings = mThreeDSecureWebView.getSettings();
        assertEquals(BraintreeHttpClient.getUserAgent(), settings.getUserAgentString());
        assertEquals(WebSettings.LOAD_CACHE_ELSE_NETWORK, settings.getCacheMode());
        assertTrue(settings.supportMultipleWindows());
        assertTrue(settings.getJavaScriptEnabled());
        assertTrue(settings.getBuiltInZoomControls());
        assertFalse(settings.getDisplayZoomControls());
        assertTrue(settings.getDomStorageEnabled());
        assertTrue(settings.getDatabaseEnabled());
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    @Test
    public void init_setsCookieManagerToAcceptThirdPartyCookies() {
        mThreeDSecureWebView.init(mActivity);

        assertTrue(CookieManager.getInstance().acceptThirdPartyCookies(mThreeDSecureWebView));
    }
}
