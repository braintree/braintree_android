package com.braintreepayments.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
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
public class LocalPaymentLauncherUnitTest {

    private BrowserSwitchClient browserSwitchClient;
    private ComponentActivity activity;
    private Intent intent;
    private BrowserSwitchRequest browserSwitchRequest;


    @Before
    public void beforeEach() {
        browserSwitchClient = mock(BrowserSwitchClient.class);
        activity = mock(ComponentActivity.class);
        intent = new Intent();
        browserSwitchRequest = mock(BrowserSwitchRequest.class);

    }

    @Test
    public void launch_startsBrowserSwitch_returnsPendingRequest() {
        LocalPaymentAuthRequestParams
                localPaymentAuthRequestParams = mock(LocalPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(localPaymentAuthRequestParams.getBrowserSwitchOptions()).thenReturn(options);
        BrowserSwitchPendingRequest browserSwitchPendingRequest =
                new BrowserSwitchPendingRequest.Started(browserSwitchRequest);
        when(browserSwitchClient.start(same(activity), same(options))).thenReturn(browserSwitchPendingRequest);
        LocalPaymentLauncher sut =
                new LocalPaymentLauncher(browserSwitchClient);

        LocalPaymentPendingRequest pendingRequest = sut.launch(activity, new LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams));

        assertTrue(pendingRequest instanceof LocalPaymentPendingRequest.Started);
        assertSame(browserSwitchPendingRequest, ((LocalPaymentPendingRequest.Started) pendingRequest).getRequest$LocalPayment_debug());
        verify(browserSwitchClient).start(same(activity), same(options));
    }

    @Test
    public void launch_onError_returnsFailure() {
        LocalPaymentAuthRequestParams
                localPaymentAuthRequestParams = mock(LocalPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        BrowserSwitchException exception = new BrowserSwitchException("error");
        when((browserSwitchClient).start(same(activity), same(options))).thenReturn(new BrowserSwitchPendingRequest.Failure(exception));
        when(localPaymentAuthRequestParams.getBrowserSwitchOptions()).thenReturn(options);
        LocalPaymentLauncher sut =
                new LocalPaymentLauncher(browserSwitchClient);

        LocalPaymentPendingRequest pendingRequest = sut.launch(activity, new LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams));

        assertTrue(pendingRequest instanceof LocalPaymentPendingRequest.Failure);
        assertSame(exception, ((LocalPaymentPendingRequest.Failure) pendingRequest).getError());
    }

    @Test
    public void handleReturnToAppFromBrowser_onBrowserSwitchResult_returnsResult() {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        BrowserSwitchPendingRequest.Started browserSwitchPendingRequest =
                new BrowserSwitchPendingRequest.Started(browserSwitchRequest);
        LocalPaymentPendingRequest.Started pendingRequest =
                new LocalPaymentPendingRequest.Started(browserSwitchPendingRequest);
        when(browserSwitchClient.parseResult(eq(browserSwitchPendingRequest),
                eq(intent))).thenReturn(browserSwitchResult);
        LocalPaymentLauncher sut =
                new LocalPaymentLauncher(browserSwitchClient);

        LocalPaymentAuthResult paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent);

        assertNotNull(paymentAuthResult);
        assertSame(paymentAuthResult.getBrowserSwitchResult(), browserSwitchResult);
    }

    @Test
    public void handleReturnToAppFromBrowser_whenNoBrowserSwitchResult_returnsNull() {
        BrowserSwitchPendingRequest.Started browserSwitchPendingRequest =
                new BrowserSwitchPendingRequest.Started(browserSwitchRequest);
        LocalPaymentPendingRequest.Started pendingRequest =
                new LocalPaymentPendingRequest.Started(browserSwitchPendingRequest);
        when(browserSwitchClient.parseResult(eq(browserSwitchPendingRequest),
                eq(intent))).thenReturn(null);
        LocalPaymentLauncher sut =
                new LocalPaymentLauncher(browserSwitchClient);

        LocalPaymentAuthResult paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent);

        assertNull(paymentAuthResult);    }
}