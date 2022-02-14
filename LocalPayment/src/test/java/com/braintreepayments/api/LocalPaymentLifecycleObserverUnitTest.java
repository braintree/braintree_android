package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LocalPaymentLifecycleObserverUnitTest {

    @Test
    public void onResume_whenLifeCycleObserverIsFragment_payPalClientDeliversResultWithFragmentActivity() {

    }

    @Test
    public void onResume_whenLifeCycleObserverIsActivity_payPalClientDeliversResultWithSameActivity() {

    }

    @Test
    public void onResume_whenPendingBrowserSwitchResultExists_andRequestCodeNotPayPal_doesNothing() {

    }
}
