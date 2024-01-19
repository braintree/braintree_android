package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.SEPA_DEBIT;
import static org.junit.Assert.assertNotNull;
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
public class SEPADirectDebitLauncherUnitTest {

    private BrowserSwitchClient browserSwitchClient;
    private BrowserSwitchRequest browserSwitchRequest;
    private ComponentActivity activity;
    private Intent intent;

    @Before
    public void beforeEach() {
        browserSwitchRequest = mock(BrowserSwitchRequest.class);
        browserSwitchClient = mock(BrowserSwitchClient.class);
        activity = mock(ComponentActivity.class);
        intent = new Intent();
    }

    @Test
    public void launch_onSuccess_startsBrowserSwitch_returnsPendingRequest() {
        SEPADirectDebitPaymentAuthRequestParams
                sepaResponse = mock(SEPADirectDebitPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(sepaResponse.getBrowserSwitchOptions()).thenReturn(options);
        BrowserSwitchPendingRequest browserSwitchPendingRequest = new BrowserSwitchPendingRequest.Started(browserSwitchRequest);
        when(browserSwitchClient.start(activity, options)).thenReturn(browserSwitchPendingRequest);
        SEPADirectDebitLauncher sut = new SEPADirectDebitLauncher(browserSwitchClient);

        SEPADirectDebitPendingRequest pendingRequest = sut.launch(activity, new SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(sepaResponse));

        verify(browserSwitchClient).start(same(activity), same(options));
        assertTrue(pendingRequest instanceof SEPADirectDebitPendingRequest.Started);
        assertSame(browserSwitchPendingRequest, ((SEPADirectDebitPendingRequest.Started) pendingRequest).getRequest().getBrowserSwitchPendingRequest());
    }

    @Test
    public void launch_onError_returnsFailure() {
        SEPADirectDebitPaymentAuthRequestParams
                sepaResponse = mock(SEPADirectDebitPaymentAuthRequestParams.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(sepaResponse.getBrowserSwitchOptions()).thenReturn(options);
        BrowserSwitchException exception = new BrowserSwitchException("error");
        BrowserSwitchPendingRequest browserSwitchPendingRequest = new BrowserSwitchPendingRequest.Failure(exception);
        when(browserSwitchClient.start(activity, options)).thenReturn(browserSwitchPendingRequest);
        SEPADirectDebitLauncher sut = new SEPADirectDebitLauncher(browserSwitchClient);

        SEPADirectDebitPendingRequest pendingRequest = sut.launch(activity, new SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(sepaResponse));

        assertTrue(pendingRequest instanceof SEPADirectDebitPendingRequest.Failure);
        assertSame(exception, ((SEPADirectDebitPendingRequest.Failure) pendingRequest).getError());
    }

    @Test
    public void handleReturnToAppFromBrowser_deliversResultToLauncherCallback() {
        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        BrowserSwitchPendingRequest.Started browserSwitchPendingRequest = new BrowserSwitchPendingRequest.Started(browserSwitchRequest);
        SEPADirectDebitPendingRequest.Started pendingRequest = new SEPADirectDebitPendingRequest.Started(new SEPADirectDebitBrowserSwitchRequest(browserSwitchPendingRequest));
        when(browserSwitchClient.parseResult(eq(browserSwitchPendingRequest), eq(intent))).thenReturn(
                browserSwitchResult);

        SEPADirectDebitLauncher sut = new SEPADirectDebitLauncher(browserSwitchClient);

        SEPADirectDebitPaymentAuthResult paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent);

        assertNotNull(paymentAuthResult);
        assertSame(paymentAuthResult.getBrowserSwitchResult(), browserSwitchResult);
    }
}
