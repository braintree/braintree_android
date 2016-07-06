package com.braintreepayments.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricGradleTestRunner.class)
public class CrashReporterUnitTest {

    private BraintreeFragment mBraintreeFragment;
    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;
    private CrashReporter mCrashReporter;

    @Before
    public void setup() {
        mDefaultUncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler);
        mBraintreeFragment = mock(BraintreeFragment.class);
        mCrashReporter = CrashReporter.setup(mBraintreeFragment);
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
    public void uncaughtException_doesNotSendCrashEventIfStackTraceDidNotContainBraintreeCode() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("test", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);

        verifyZeroInteractions(mBraintreeFragment);
    }

    @Test
    public void uncaughtException_forwardsExceptionToDefaultUncaughtExceptionHandlerForNonBraintreeCrashes() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("test", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);

        verify(mDefaultUncaughtExceptionHandler).uncaughtException(null, exception);
    }

    @Test
    public void uncaughtException_sendsCrashEventIfStackTraceContainsBraintreeCode() {
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.braintreepayments.api.CrashReporting", "test", "test", 1) });
        mCrashReporter.uncaughtException(null, exception);

        exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.paypal.CrashReporting", "test", "test", 1) });
        mCrashReporter.uncaughtException(null, exception);

        verify(mBraintreeFragment, times(2)).sendAnalyticsEvent("crash");
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
        mCrashReporter = CrashReporter.setup(mBraintreeFragment);
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.braintreepayments.api.CrashReporting", "test", "test", 1) });

        mCrashReporter.uncaughtException(null, exception);
    }
}
