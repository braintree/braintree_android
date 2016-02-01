package com.braintreepayments.api.threedsecure;

import android.net.http.SslError;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;

import com.braintreepayments.api.models.ThreeDSecureAuthenticationResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureWebViewClientTest {

    private ThreeDSecureWebViewActivity mActivity;
    private WebView mWebView;
    private ThreeDSecureWebViewClient mWebViewClient;

    @Before
    public void setUp() {
        mActivity = mock(ThreeDSecureWebViewActivity.class);
        mWebView = mock(WebView.class);
        mWebViewClient = new ThreeDSecureWebViewClient(mActivity);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onPageStarted_callsFinishOnLastPage() {
        mWebViewClient.onPageStarted(mWebView, "http://test.com/html/authentication_complete_frame?auth_response={}", null);

        verify(mWebView, times(1)).stopLoading();
        verify(mActivity, times(1)).finishWithResult(any(ThreeDSecureAuthenticationResponse.class));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onPageFinished_setsTheActivityTitle() {
        stub(mWebView.getTitle()).toReturn("TEST");

        mWebViewClient.onPageFinished(mWebView, null);

        verify(mActivity, times(1)).setActionBarTitle("TEST");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onReceievedError_callsFinish() {
        mWebViewClient.onReceivedError(mWebView, 0, "TEST", "");

        verify(mWebView, times(1)).stopLoading();
        verify(mActivity, times(1)).finishWithResult(any(ThreeDSecureAuthenticationResponse.class));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onReceivedSslError_callsFinish() {
        SslErrorHandler handler = mock(SslErrorHandler.class);
        mWebViewClient.onReceivedSslError(mWebView, handler, mock(SslError.class));

        verify(mWebView, times(1)).stopLoading();
        verify(handler, times(1)).cancel();
        verify(mActivity, times(1)).finishWithResult(any(ThreeDSecureAuthenticationResponse.class));
    }
}
