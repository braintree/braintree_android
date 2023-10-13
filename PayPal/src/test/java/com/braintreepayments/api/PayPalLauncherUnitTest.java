package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PayPalLauncherUnitTest {

    private BrowserSwitchClient browserSwitchClient;
    private FragmentActivity activity;
    private Intent intent;

    @Before
    public void beforeEach() {
        browserSwitchClient = mock(BrowserSwitchClient.class);
        activity = mock(FragmentActivity.class);
        intent = new Intent();

    }

    @Test
    public void launch_startsBrowserSwitch() throws BrowserSwitchException {
        PayPalResponse payPalResponse = mock(PayPalResponse.class);
        BrowserSwitchOptions options = mock(BrowserSwitchOptions.class);
        when(payPalResponse.getBrowserSwitchOptions()).thenReturn(options);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient);

        sut.launch(activity, payPalResponse);

        verify(browserSwitchClient).start(same(activity), same(options));
    }

    @Test
    public void deliverResult_returnsResultFromBrowserSwitchClient() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(browserSwitchClient.parseResult(eq(activity), eq(PAYPAL), eq(intent))).thenReturn(
                result);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient);

        assertSame(result, sut.deliverResult(activity, intent));
    }

    @Test
    public void deliverResult_clearsActiveBrowserSwitchRequests() {
        BrowserSwitchResult result = mock(BrowserSwitchResult.class);
        when(browserSwitchClient.parseResult(eq(activity), eq(PAYPAL), eq(intent))).thenReturn(
                result);
        PayPalLauncher sut = new PayPalLauncher(browserSwitchClient);

        sut.deliverResult(activity, intent);

        verify(browserSwitchClient).clearActiveRequests(same(activity));
    }
}
