package com.braintreepayments.api;

import static android.os.Looper.getMainLooper;
import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;
import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

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
        when(payPalClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);
        sut.onStateChanged(fragment, Lifecycle.Event.ON_RESUME);

        // Ref: https://robolectric.org/blog/2019/06/04/paused-looper/
        shadowOf(getMainLooper()).idle();
        verify(payPalClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsActivity_payPalClientDeliversResultWithSameActivity() {
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(PAYPAL);

        PayPalClient payPalClient = mock(PayPalClient.class);
        when(payPalClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(payPalClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(payPalClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsFragment_payPalClientDeliversResultFromCacheWithFragmentActivity() {
        Fragment fragment = mock(Fragment.class);
        FragmentActivity activity = mock(FragmentActivity.class);
        when(fragment.getActivity()).thenReturn(activity);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(PAYPAL);

        PayPalClient payPalClient = mock(PayPalClient.class);
        when(payPalClient.getBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);
        when(payPalClient.deliverBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);
        sut.onStateChanged(fragment, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(payPalClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenLifeCycleObserverIsActivity_payPalClientDeliversResultFromCacheWithSameActivity() {
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(PAYPAL);

        PayPalClient payPalClient = mock(PayPalClient.class);
        when(payPalClient.getBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);
        when(payPalClient.deliverBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(payPalClient).onBrowserSwitchResult(same(browserSwitchResult));
    }

    @Test
    public void onResume_whenPendingBrowserSwitchResultExists_andRequestCodeNotPayPal_doesNothing() {
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(THREE_D_SECURE);

        PayPalClient payPalClient = mock(PayPalClient.class);
        when(payPalClient.getBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);
        when(payPalClient.deliverBrowserSwitchResult(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(payPalClient, never()).onBrowserSwitchResult(any(BrowserSwitchResult.class));
    }

    @Test
    public void onResume_whenCachedBrowserSwitchResultExists_andRequestCodeNotPayPal_doesNothing() {
        FragmentActivity activity = mock(FragmentActivity.class);

        BrowserSwitchResult browserSwitchResult = mock(BrowserSwitchResult.class);
        when(browserSwitchResult.getRequestCode()).thenReturn(THREE_D_SECURE);

        PayPalClient payPalClient = mock(PayPalClient.class);
        when(payPalClient.getBrowserSwitchResultFromNewTask(activity)).thenReturn(browserSwitchResult);

        PayPalLifecycleObserver sut = new PayPalLifecycleObserver(payPalClient);
        sut.onStateChanged(activity, Lifecycle.Event.ON_RESUME);

        shadowOf(getMainLooper()).idle();
        verify(payPalClient, never()).onBrowserSwitchResult(any(BrowserSwitchResult.class));
    }
}