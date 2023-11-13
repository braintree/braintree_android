package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.LOCAL_PAYMENT;
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
public class LocalPaymentLauncherUnitTest {

    private BrowserSwitchClient browserSwitchClient;
    private FragmentActivity activity;
    private Intent intent;

    private LocalPaymentLauncherCallback localPaymentLauncherCallback;

    @Before
    public void beforeEach() {
        browserSwitchClient = mock(BrowserSwitchClient.class);
        activity = mock(FragmentActivity.class);
        intent = new Intent();
        localPaymentLauncherCallback = mock(LocalPaymentLauncherCallback.class);

    }

    @Test
    public void launch_startsBrowserSwitch() throws BrowserSwitchException {
        LocalPaymentAuthRequest localPaymentAuthRequest = mock(LocalPaymentAuthRequest.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(localPaymentAuthRequest.getBrowserSwitchOptions()).thenReturn(options);
        LocalPaymentLauncher sut =
                new LocalPaymentLauncher(browserSwitchClient, localPaymentLauncherCallback);

        sut.launch(activity, localPaymentAuthRequest);

        verify(browserSwitchClient).start(same(activity), same(options));
    }

    @Test
    public void launch_onError_callsBackError() throws BrowserSwitchException {
        LocalPaymentAuthRequest localPaymentAuthRequest = mock(LocalPaymentAuthRequest.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        BrowserSwitchException exception = new BrowserSwitchException("error");
        doThrow(exception).when(browserSwitchClient).start(same(activity), same(options));
        when(localPaymentAuthRequest.getBrowserSwitchOptions()).thenReturn(options);
        LocalPaymentLauncher sut =
                new LocalPaymentLauncher(browserSwitchClient, localPaymentLauncherCallback);

        sut.launch(activity, localPaymentAuthRequest);

        ArgumentCaptor<LocalPaymentAuthResult> captor =
                ArgumentCaptor.forClass(LocalPaymentAuthResult.class);
        verify(localPaymentLauncherCallback).onResult(captor.capture());
        assertSame(exception, captor.getValue().getError());
        assertNull(captor.getValue().getBrowserSwitchResult());
    }

    @Test
    public void deliverResult_deliversResultToLauncherCallback() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(browserSwitchClient.parseResult(eq(activity), eq(LOCAL_PAYMENT),
                eq(intent))).thenReturn(
                result);
        LocalPaymentLauncher sut =
                new LocalPaymentLauncher(browserSwitchClient, localPaymentLauncherCallback);

        sut.handleReturnToAppFromBrowser(activity, intent);

        ArgumentCaptor<LocalPaymentAuthResult> captor =
                ArgumentCaptor.forClass(LocalPaymentAuthResult.class);
        verify(localPaymentLauncherCallback).onResult(captor.capture());
        assertSame(result, captor.getValue().getBrowserSwitchResult());
        assertNull(captor.getValue().getError());
    }

    @Test
    public void handleReturnToAppFromBrowser_clearsActiveBrowserSwitchRequests() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(browserSwitchClient.parseResult(eq(activity), eq(LOCAL_PAYMENT),
                eq(intent))).thenReturn(
                result);
        LocalPaymentLauncher sut =
                new LocalPaymentLauncher(browserSwitchClient, localPaymentLauncherCallback);

        sut.handleReturnToAppFromBrowser(activity, intent);

        verify(browserSwitchClient).clearActiveRequests(same(activity));
    }
}