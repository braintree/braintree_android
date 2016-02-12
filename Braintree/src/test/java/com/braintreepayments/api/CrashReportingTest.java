package com.braintreepayments.api;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class CrashReportingTest {

    private SharedPreferences mPreferences;

    @Before
    public void setup() {
        mPreferences = BraintreeSharedPreferences.getSharedPreferences(RuntimeEnvironment.application);
        mPreferences.edit()
                .clear()
                .commit();
    }

    @Test
    public void persistBraintreeCrash_doesNotPersistCrashIfStackTraceDidNotContainBraintreeCode() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("test", "test", "test", 1) });
        CrashReporting.persistBraintreeCrash(RuntimeEnvironment.application, null, exception);

        assertFalse(mPreferences.getBoolean("com.braintreepayments.api.CrashReporting.CRASH", false));
    }

    @Test
    public void persistBraintreeCrash_persistsCrashIfStackTraceContainsBraintreeCode() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.braintreepayments.api.CrashReporting", "test", "test", 1) });
        CrashReporting.persistBraintreeCrash(RuntimeEnvironment.application, null, exception);

        assertTrue(mPreferences.getBoolean("com.braintreepayments.api.CrashReporting.CRASH", false));
    }

    @Test
    public void sendPreviousCrashes_sendsCrashIfThereWasAPreviousCrash() {
        BraintreeFragment fragment = mock(BraintreeFragment.class);
        when(fragment.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        mPreferences.edit().putBoolean("com.braintreepayments.api.CrashReporting.CRASH", true).commit();

        CrashReporting.sendPreviousCrashes(fragment);

        verify(fragment).sendAnalyticsEvent("crash");
    }

    @Test
    public void sendPreviousCrashes_doesNothingIfThereWasNoPreviousCrash() {
        BraintreeFragment fragment = mock(BraintreeFragment.class);
        when(fragment.getApplicationContext()).thenReturn(RuntimeEnvironment.application);

        CrashReporting.sendPreviousCrashes(fragment);

        verify(fragment, never()).sendAnalyticsEvent(anyString());
    }
}
