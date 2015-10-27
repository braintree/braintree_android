package com.braintreepayments.api;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.interfaces.BraintreeCancelListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BrowserSwitchStateTest {

    private static final int BROWSER_SWITCH_REQUEST = Integer.MAX_VALUE;
    private BrowserSwitchState mBrowserSwitchState;

    @Before
    public void createBrowserSwitchState(){
        mBrowserSwitchState = new BrowserSwitchState();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void start_finishReturnsRequestCode(){
        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);
        assertEquals(BROWSER_SWITCH_REQUEST, mBrowserSwitchState.end());
    }

    @Test(timeout = 1000)
    @SmallTest
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
    @SmallTest
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
    @SmallTest
    public void start_setsStartStateCorrectly() {
        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);

        assertFalse(mBrowserSwitchState.mBrowserSwitchHasReturned);
        assertEquals(BROWSER_SWITCH_REQUEST, mBrowserSwitchState.mRequestCode);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void onResume_changesToProperState() {
        BraintreeFragment fragment = new BraintreeFragment();
        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);
        mBrowserSwitchState.checkForCancel(fragment);

        assertNull(mBrowserSwitchState.mBrowserSwitchHasReturned);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void finish_resetsStateCorrectly() {
        BraintreeFragment fragment = new BraintreeFragment();
        mBrowserSwitchState.begin(BROWSER_SWITCH_REQUEST);
        mBrowserSwitchState.checkForCancel(fragment);
        int code = mBrowserSwitchState.end();

        assertEquals(BROWSER_SWITCH_REQUEST, code);
        assertTrue(mBrowserSwitchState.mBrowserSwitchHasReturned);
    }
}
