package com.braintreepayments.api.threedsecure;

import android.os.Build.VERSION_CODES;
import android.support.annotation.RequiresApi;
import android.webkit.CookieManager;

import com.braintreepayments.api.shadows.BraintreeShadowCookieManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(sdk = 21, shadows = BraintreeShadowCookieManager.class)
@RunWith(RobolectricGradleTestRunner.class)
public class ThreeDSecureWebViewUnitTest {

    private ThreeDSecureWebView mThreeDSecureWebView;
    private ThreeDSecureWebViewActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mock(ThreeDSecureWebViewActivity.class);
        when(mActivity.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        mThreeDSecureWebView = new ThreeDSecureWebView(RuntimeEnvironment.application);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    @Test
    public void init_setsCookieManagerToAcceptThirdPartyCookies() {
        mThreeDSecureWebView.init(mActivity);

        assertTrue(CookieManager.getInstance().acceptThirdPartyCookies(mThreeDSecureWebView));
    }
}
