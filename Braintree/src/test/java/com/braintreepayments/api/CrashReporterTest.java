package com.braintreepayments.api;

import android.content.SharedPreferences;

import com.braintreepayments.api.internal.BraintreeSharedPreferences;

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

@RunWith(RobolectricGradleTestRunner.class)
public class CrashReporterTest {

    private SharedPreferences mPreferences;
    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;
    private CrashReporter mCrashReporter;

    @Before
    public void setup() {
        mPreferences = BraintreeSharedPreferences.getSharedPreferences(RuntimeEnvironment.application);
        mPreferences.edit()
                .clear()
                .commit();

        mDefaultUncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler);
        mCrashReporter = CrashReporter.setup(RuntimeEnvironment.application);
    }

    @Test
    public void setup_setsCrashReporterAsTheDefaultUncaughtExceptionHandler() {
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashReporter);
    }

    @Test
    public void tearDown_setsDefaultUncaughtExceptionHandlerBack() {
        mCrashReporter.tearDown();

        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashReporter);
    }

    @Test
    public void uncaughtException_doesNotPersistCrashIfStackTraceDidNotContainBraintreeCode() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("test", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);

        assertFalse(mPreferences.getBoolean("com.braintreepayments.api.CrashReporting.CRASH", false));
    }

    @Test
    public void uncaughtException_forwardsExceptionToDefaultUncaughtExceptionHandlerForNonBraintreeCrashes() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("test", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);

        verify(mDefaultUncaughtExceptionHandler).uncaughtException(null, exception);
    }

    @Test
    public void uncaughtException_persistsCrashIfStackTraceContainsBraintreeCode() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.braintreepayments.api.CrashReporting", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);

        assertTrue(mPreferences.getBoolean("com.braintreepayments.api.CrashReporting.CRASH", false));

        mPreferences.edit().clear().commit();
        exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.paypal.CrashReporting", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);

        assertTrue(mPreferences.getBoolean("com.braintreepayments.api.CrashReporting.CRASH", false));
    }

    @Test
    public void uncaughtException_forwardsExceptionToDefaultUncaughtExceptionHandlerForBraintreeCrashes() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.braintreepayments.api.CrashReporting", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);

        verify(mDefaultUncaughtExceptionHandler).uncaughtException(null, exception);
    }

    @Test
    public void uncaughtException_doesNotCrashIfDefaultUncaughtExceptionHandlerWasNull() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        mCrashReporter = CrashReporter.setup(RuntimeEnvironment.application);
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.braintreepayments.api.CrashReporting", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);
    }

    @Test
    public void sendPreviousCrashes_sendsCrashIfThereWasAPreviousCrash() {
        BraintreeFragment fragment = mock(BraintreeFragment.class);
        mPreferences.edit().putBoolean("com.braintreepayments.api.CrashReporting.CRASH", true).commit();

        mCrashReporter.sendPreviousCrashes(fragment);

        verify(fragment).sendAnalyticsEvent("crash");
    }

    @Test
    public void sendPreviousCrashes_doesNothingIfThereWasNoPreviousCrash() {
        BraintreeFragment fragment = mock(BraintreeFragment.class);

        mCrashReporter.sendPreviousCrashes(fragment);

        verify(fragment, never()).sendAnalyticsEvent(anyString());
    }
}
