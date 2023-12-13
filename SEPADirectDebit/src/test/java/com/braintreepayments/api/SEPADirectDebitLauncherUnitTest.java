package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.SEPA_DEBIT;
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
public class SEPADirectDebitLauncherUnitTest {

    private BrowserSwitchClient browserSwitchClient;
    private FragmentActivity activity;
    private Intent intent;
    private SEPADirectDebitLauncherCallback sepaLauncherCallback;

    @Before
    public void beforeEach() {
        browserSwitchClient = mock(BrowserSwitchClient.class);
        activity = mock(FragmentActivity.class);
        intent = new Intent();
        sepaLauncherCallback = mock(SEPADirectDebitLauncherCallback.class);
    }

    @Test
    public void launch_startsBrowserSwitch() throws BrowserSwitchException {
        SEPADirectDebitPaymentAuthRequestParams
                sepaResponse = mock(SEPADirectDebitPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(sepaResponse.getBrowserSwitchOptions()).thenReturn(options);
        SEPADirectDebitLauncher sut = new SEPADirectDebitLauncher(browserSwitchClient, sepaLauncherCallback);

        sut.launch(activity, new SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(sepaResponse));

        verify(browserSwitchClient).start(same(activity), same(options));
    }

    @Test
    public void launch_onError_callsBackError() throws BrowserSwitchException {
        SEPADirectDebitPaymentAuthRequestParams
                sepaResponse = mock(SEPADirectDebitPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(sepaResponse.getBrowserSwitchOptions()).thenReturn(options);
        BrowserSwitchException exception = new BrowserSwitchException("error");
        doThrow(exception).when(browserSwitchClient).start(same(activity), same(options));
        SEPADirectDebitLauncher sut = new SEPADirectDebitLauncher(browserSwitchClient, sepaLauncherCallback);

        sut.launch(activity, new SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(sepaResponse));

        ArgumentCaptor<SEPADirectDebitPaymentAuthResult> captor =
                ArgumentCaptor.forClass(SEPADirectDebitPaymentAuthResult.class);
        verify(sepaLauncherCallback).onResult(captor.capture());
        assertSame(exception, captor.getValue().getError());
        assertNull(captor.getValue().getBrowserSwitchResult());
    }

    @Test
    public void handleReturnToAppFromBrowser_deliversResultToLauncherCallback() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(browserSwitchClient.parseResult(eq(activity), eq(SEPA_DEBIT), eq(intent))).thenReturn(
                result);
        SEPADirectDebitLauncher sut = new SEPADirectDebitLauncher(browserSwitchClient, sepaLauncherCallback);

        sut.handleReturnToAppFromBrowser(activity, intent);

        ArgumentCaptor<SEPADirectDebitPaymentAuthResult> captor =
                ArgumentCaptor.forClass(SEPADirectDebitPaymentAuthResult.class);
        verify(sepaLauncherCallback).onResult(captor.capture());
        assertSame(result, captor.getValue().getBrowserSwitchResult());
        assertNull(captor.getValue().getError());
    }

    @Test
    public void handleReturnToAppFromBrowser_clearsActiveBrowserSwitchRequests() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(browserSwitchClient.parseResult(eq(activity), eq(SEPA_DEBIT), eq(intent))).thenReturn(
                result);
        SEPADirectDebitLauncher sut = new SEPADirectDebitLauncher(browserSwitchClient, sepaLauncherCallback);

        sut.handleReturnToAppFromBrowser(activity, intent);

        verify(browserSwitchClient).clearActiveRequests(same(activity));
    }
}
