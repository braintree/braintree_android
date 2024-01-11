package com.braintreepayments.api;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import androidx.activity.ComponentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PayPalLauncherUnitTest {

    private BrowserSwitchClient browserSwitchClient;
    private ComponentActivity activity;
    private Intent intent;


    @Before
    public void beforeEach() {
        browserSwitchClient = mock(BrowserSwitchClient.class);
        activity = mock(ComponentActivity.class);
        intent = new Intent();

    }

    @Test
    public void launch_startsBrowserSwitch_returnsPendingRequest() throws BrowserSwitchException {
        PayPalPaymentAuthRequestParams paymentAuthRequestParams = mock(PayPalPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(paymentAuthRequestParams.getBrowserSwitchOptions()).thenReturn(options);
        BrowserSwitchRequest browserSwitchRequest = mock(BrowserSwitchRequest.class);
        when(browserSwitchClient.start(activity, options)).thenReturn(browserSwitchRequest);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient);

        PayPalPendingRequest pendingRequest = sut.launch(activity, new PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams));

        assertTrue(pendingRequest instanceof PayPalPendingRequest.Started);
        assertEquals(browserSwitchRequest, ((PayPalPendingRequest.Started) pendingRequest).getRequest().getBrowserSwitchRequest());
        verify(browserSwitchClient).start(same(activity), same(options));
    }

    @Test
    public void launch_onError_returnsPendingRequestFailure() throws BrowserSwitchException {
        PayPalPaymentAuthRequestParams paymentAuthRequestParams = mock(PayPalPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(paymentAuthRequestParams.getBrowserSwitchOptions()).thenReturn(options);
        BrowserSwitchException exception = new BrowserSwitchException("error");
        doThrow(exception).when(browserSwitchClient).start(same(activity), same(options));
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient);

        PayPalPendingRequest pendingRequest = sut.launch(activity, new PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams));

        assertTrue(pendingRequest instanceof PayPalPendingRequest.Failure);
        assertSame(exception, ((PayPalPendingRequest.Failure) pendingRequest).getError());
    }

    @Test
    public void launch_whenDeviceCantPerformBrowserSwitch_returnsPendingRequestFailure()
            throws BrowserSwitchException {
        PayPalPaymentAuthRequestParams paymentAuthRequestParams = mock(PayPalPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(paymentAuthRequestParams.getBrowserSwitchOptions()).thenReturn(options);
        BrowserSwitchException exception = new BrowserSwitchException("browser switch error");
        doThrow(exception).when(browserSwitchClient).assertCanPerformBrowserSwitch(same(activity), same(options));
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient);

        PayPalPendingRequest pendingRequest = sut.launch(activity, new PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams));

        assertTrue(pendingRequest instanceof PayPalPendingRequest.Failure);
        assertEquals("AndroidManifest.xml is incorrectly configured or another app " +
                        "defines the same browser switch url as this app. See " +
                        "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup " +
                        "for the correct configuration: browser switch error",
               ((PayPalPendingRequest.Failure) pendingRequest).getError().getMessage());
    }

    @Test
    public void handleReturnToAppFromBrowser_whenResultExist_returnsResult() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        BrowserSwitchRequest browserSwitchRequest = mock(BrowserSwitchRequest.class);
        PayPalBrowserSwitchRequest payPalBrowserSwitchRequest = new PayPalBrowserSwitchRequest(browserSwitchRequest);
        when(browserSwitchClient.parseResult(browserSwitchRequest, intent)).thenReturn(result);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient);

        PayPalPaymentAuthResult paymentAuthResult = sut.handleReturnToAppFromBrowser(new PayPalPendingRequest.Started(payPalBrowserSwitchRequest), intent);

        assertNotNull(paymentAuthResult);
        assertSame(result, paymentAuthResult.getBrowserSwitchResult());
    }

    @Test
    public void handleReturnToAppFromBrowser_whenResultDoesNotExist_returnsNull() {
        BrowserSwitchRequest browserSwitchRequest = mock(BrowserSwitchRequest.class);
        PayPalBrowserSwitchRequest payPalBrowserSwitchRequest = new PayPalBrowserSwitchRequest(browserSwitchRequest);
        when(browserSwitchClient.parseResult(browserSwitchRequest, intent)).thenReturn(null);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient);

        PayPalPaymentAuthResult paymentAuthResult = sut.handleReturnToAppFromBrowser(new PayPalPendingRequest.Started(payPalBrowserSwitchRequest), intent);

        assertNull(paymentAuthResult);
    }
}
