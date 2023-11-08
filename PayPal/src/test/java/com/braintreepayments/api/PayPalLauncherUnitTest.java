package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PayPalLauncherUnitTest {

    private BrowserSwitchClient browserSwitchClient;
    private FragmentActivity activity;
    private Intent intent;

    private PayPalLauncherCallback payPalLauncherCallback;

    @Before
    public void beforeEach() {
        browserSwitchClient = mock(BrowserSwitchClient.class);
        activity = mock(FragmentActivity.class);
        intent = new Intent();
        payPalLauncherCallback = mock(PayPalLauncherCallback.class);

    }

    @Test
    public void launch_startsBrowserSwitch() throws BrowserSwitchException {
        PayPalPaymentAuthRequest paymentAuthRequest = mock(PayPalPaymentAuthRequest.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(paymentAuthRequest.getBrowserSwitchOptions()).thenReturn(options);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient, payPalLauncherCallback);

        sut.launch(activity, paymentAuthRequest);

        verify(browserSwitchClient).start(same(activity), same(options));
    }

    @Test
    public void launch_onError_callsBackError() throws BrowserSwitchException {
        PayPalPaymentAuthRequest paymentAuthRequest = mock(PayPalPaymentAuthRequest.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(paymentAuthRequest.getBrowserSwitchOptions()).thenReturn(options);
        BrowserSwitchException exception = new BrowserSwitchException("error");
        doThrow(exception).when(browserSwitchClient).start(same(activity), same(options));
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient, payPalLauncherCallback);

        sut.launch(activity, paymentAuthRequest);

        ArgumentCaptor<PayPalPaymentAuthResult> captor =
                ArgumentCaptor.forClass(PayPalPaymentAuthResult.class);
        verify(payPalLauncherCallback).onResult(captor.capture());
        assertSame(exception, captor.getValue().getError());
        assertNull(captor.getValue().getBrowserSwitchResult());
    }

    @Test
    public void deliverResult_deliversResultToLauncherCallback() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(browserSwitchClient.parseResult(eq(activity), eq(PAYPAL), eq(intent))).thenReturn(
                result);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient, payPalLauncherCallback);

        sut.handleReturnToAppFromBrowser(activity, intent);

        ArgumentCaptor<PayPalPaymentAuthResult> captor =
                ArgumentCaptor.forClass(PayPalPaymentAuthResult.class);
        verify(payPalLauncherCallback).onResult(captor.capture());
        assertSame(result, captor.getValue().getBrowserSwitchResult());
        assertNull(captor.getValue().getError());
    }

    @Test
    public void handleReturnToAppFromBrowser_clearsActiveBrowserSwitchRequests() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(browserSwitchClient.parseResult(eq(activity), eq(PAYPAL), eq(intent))).thenReturn(
                result);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient, payPalLauncherCallback);

        sut.handleReturnToAppFromBrowser(activity, intent);

        verify(browserSwitchClient).clearActiveRequests(same(activity));
    }
}
