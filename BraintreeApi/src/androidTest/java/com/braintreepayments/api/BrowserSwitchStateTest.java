package com.braintreepayments.api;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.interfaces.BraintreeCancelListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BrowserSwitchStateTest {

    private static final int BROWSER_SWITCH_REQUEST = Integer.MAX_VALUE;
    private BrowserSwitchState mBrowserSwitchState;

    @Before
    public void createBrowserSwitchState(){
        mBrowserSwitchState = new BrowserSwitchState();
    }

    @Test(timeout = 1000)
    public void start_finishReturnsRequestCode() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN");
        assertTrue(uri.getPath().contains("success"));
        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);

        assertEquals(BROWSER_SWITCH_REQUEST, mBrowserSwitchState.end());
    }

    @Test(timeout = 1000)
    public void onResume_doesntCallCancelWhenNoSwitch(){
        BraintreeFragment fragment = new BraintreeFragment();

        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                fail("Called onCancel");
            }
        });

        mBrowserSwitchState.checkForCancel(fragment);
    }

    @Test(timeout = 1000)
    public void onResume_CallCancelWhenNoSwitch() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeFragment fragment = new BraintreeFragment();

        fragment.addListener(new BraintreeCancelListener() {
            @Override
            public void onCancel(int requestCode) {
                latch.countDown();
            }
        });

        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);
        mBrowserSwitchState.checkForCancel(fragment);
        latch.await();
    }

    @Test(timeout = 1000)
    public void start_setsStartStateCorrectly() {
        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);

        assertTrue(mBrowserSwitchState.isInFlight());
        assertEquals(BROWSER_SWITCH_REQUEST, mBrowserSwitchState.end());
    }

    @Test(timeout = 1000)
    public void onResume_changesToProperState() {
        BraintreeFragment fragment = new BraintreeFragment();

        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);
        mBrowserSwitchState.checkForCancel(fragment);

        assertFalse(mBrowserSwitchState.isInFlight());
    }

    @Test(timeout = 1000)
    public void end_resetsStateCorrectly() {
        BraintreeFragment fragment = new BraintreeFragment();

        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);
        mBrowserSwitchState.checkForCancel(fragment);
        int code = mBrowserSwitchState.end();

        assertEquals(BROWSER_SWITCH_REQUEST, code);
        assertFalse(mBrowserSwitchState.isInFlight());
    }
}
