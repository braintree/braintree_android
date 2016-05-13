package com.braintreepayments.api.threedsecure;

import android.os.Message;
import android.webkit.WebView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class ThreeDSecureWebChromeClientUnitTest {

    private ThreeDSecureWebViewActivity mActivity;
    private ThreeDSecureWebChromeClient mThreeDSecureWebChromeClient;

    @Before
    public void setup() {
        mActivity = mock(ThreeDSecureWebViewActivity.class);
        when(mActivity.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        mThreeDSecureWebChromeClient = new ThreeDSecureWebChromeClient(mActivity);
    }

    @Test
    public void onCreateWindow_pushesNewWebView() {
        WebView.WebViewTransport webViewTransport = mock(WebView.WebViewTransport.class);
        Message message = mock(Message.class);
        message.obj = webViewTransport;

        boolean result = mThreeDSecureWebChromeClient.onCreateWindow(null, false, false, message);

        assertTrue(result);
        verify(mActivity).pushNewWebView(any(ThreeDSecureWebView.class));
        verify(webViewTransport).setWebView(any(ThreeDSecureWebView.class));
        verify(message).sendToTarget();
    }

    @Test
    public void onCloseWindow_popsCurrentWebView() {
        mThreeDSecureWebChromeClient.onCloseWindow(null);

        verify(mActivity).popCurrentWebView();
    }

    @Test
    public void onProgressChanged_showsProgressBar() {
        mThreeDSecureWebChromeClient.onProgressChanged(null, 42);

        verify(mActivity).setProgress(42);
        verify(mActivity).setProgressBarVisibility(true);
    }

    @Test
    public void onProgressChanged_hidesProgressBarWhenComplete() {
        mThreeDSecureWebChromeClient.onProgressChanged(null, 100);

        verify(mActivity).setProgressBarVisibility(false);
    }
}
