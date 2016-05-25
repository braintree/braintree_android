package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

import com.paypal.android.sdk.onetouch.core.CheckoutRequest;
import com.paypal.android.sdk.onetouch.core.Result;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.config.ConfigManager;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.enums.ResponseType;
import com.paypal.android.sdk.onetouch.core.fpti.TrackingPoint;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.paypal.android.sdk.onetouch.core.test.TestSetupHelper.getMockContextInspector;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class BrowserSwitchHelperTest {

    private ContextInspector mContextInspector;
    private ConfigManager mConfigManager;

    @Before
    public void setup() throws NameNotFoundException {
        mContextInspector = getMockContextInspector();
        mConfigManager = new ConfigManager(mContextInspector, mock(PayPalHttpClient.class));
        mConfigManager.useHardcodedConfig(true);
    }

    @Test
    public void getBrowserIntent_returnsIntent() throws Exception {
        CheckoutRequest request = spy(new CheckoutRequest());
        doNothing().when(request).trackFpti(any(Context.class), any(TrackingPoint.class),
                any(Protocol.class));
        request.approvalURL("https://paypal.com/?token=test-token-key");

        Intent intent = BrowserSwitchHelper.getBrowserSwitchIntent(mContextInspector, mConfigManager, request);

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.SwitchToBrowser), any(Protocol.class));

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("https://paypal.com/?token=test-token-key", intent.getData().toString());
        assertEquals("com.android.chrome", intent.getPackage());
    }

    @Test
    public void handleBrowserResponse_parsesResponse() {
        Result expectedResult = new Result();
        CheckoutRequest request = mock(CheckoutRequest.class);
        when(request.parseBrowserResponse(any(ContextInspector.class), any(Uri.class)))
                .thenReturn(expectedResult);

        Result result = BrowserSwitchHelper.parseBrowserSwitchResponse(mContextInspector, request,
                mock(Uri.class));

        assertEquals(expectedResult, result);
    }

    @Test
    public void handleBrowserResponse_sendsEventForError() {
        Result expectedResult = new Result(new Exception());
        CheckoutRequest request = mock(CheckoutRequest.class);
        when(request.parseBrowserResponse(any(ContextInspector.class), any(Uri.class)))
                .thenReturn(expectedResult);

        BrowserSwitchHelper.parseBrowserSwitchResponse(mContextInspector, request, mock(Uri.class));

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.Error),
                isNull(Protocol.class));
    }

    @Test
    public void handleBrowserResponse_sendsEventForCancel() {
        Result expectedResult = new Result();
        CheckoutRequest request = mock(CheckoutRequest.class);
        when(request.parseBrowserResponse(any(ContextInspector.class), any(Uri.class)))
                .thenReturn(expectedResult);

        BrowserSwitchHelper.parseBrowserSwitchResponse(mContextInspector, request, mock(Uri.class));

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.Cancel),
                isNull(Protocol.class));
    }

    @Test
    public void handleBrowserResponse_sendsEventForReturn() {
        Result expectedResult = new Result("test", ResponseType.web, new JSONObject(), "");
        CheckoutRequest request = mock(CheckoutRequest.class);
        when(request.parseBrowserResponse(any(ContextInspector.class), any(Uri.class)))
                .thenReturn(expectedResult);

        BrowserSwitchHelper.parseBrowserSwitchResponse(mContextInspector, request, mock(Uri.class));

        verify(request).trackFpti(any(Context.class), eq(TrackingPoint.Return),
                isNull(Protocol.class));
    }
}
