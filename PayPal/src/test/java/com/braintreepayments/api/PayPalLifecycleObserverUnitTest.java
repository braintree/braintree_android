package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;
import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PayPalLifecycleObserverUnitTest {

    @Test
    public void onResume_whenLifeCycleObserverIsFragment_payPalClientDeliversResultWithFragmentActivity() {
        Fragment fragment = mock(Fragment.class);
        FragmentActivity activity = mock(FragmentActivity.class);
        when(fragment.getActivity()).thenReturn(activity);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(PAYPAL);

        PayPalClient payPalClient = mock(PayPalClient.class);
        when(payPalClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);

        sut.onStateChanged(fragment, Lifecycle.Event.ON_RESUME);

        verify(payPalClient).onBrowserSwitchResult(same(activity));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsActivity_payPalClientDeliversResultWithSameActivity() {
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(PAYPAL);

        PayPalClient payPalClient = mock(PayPalClient.class);
        when(payPalClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);

        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(payPalClient).onBrowserSwitchResult(same(activity));
    }

    @Test
    public void onResume_whenPendingBrowserSwitchResultExists_andRequestCodeNotPayPal_doesNothing() {
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(THREE_D_SECURE);

        PayPalClient payPalClient = mock(PayPalClient.class);
        when(payPalClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);

        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        verify(payPalClient, never()).onBrowserSwitchResult(any(FragmentActivity.class));
    }
}