package com.braintreepayments.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.ref.WeakReference;

import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class CrashReporterUnitTest {

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;
    private BraintreeClient braintreeClient;

    @Before
    public void setup() {
        braintreeClient = mock(BraintreeClient.class);
        defaultExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
    }

    @Test
    public void start_setsSelfAsThreadDefaultExceptionHandler() {
        CrashReporter sut = new CrashReporter(braintreeClient);

        sut.start();
        assertSame(sut, Thread.getDefaultUncaughtExceptionHandler());
    }

    @Test
    public void uncaughtExceptionHandler_whenClientReferenceNull_forwardsInvocationToDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler);

        Thread thread = mock(Thread.class);
        Exception exception = new Exception("error");

        CrashReporter sut = new CrashReporter(new WeakReference<BraintreeClient>(null));
        sut.start();
        sut.uncaughtException(thread, exception);

        verify(defaultExceptionHandler).uncaughtException(thread, exception);
    }

    @Test
    public void uncaughtExceptionHandler_whenClientReferenceNull_restoresOriginalDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler);

        CrashReporter sut = new CrashReporter(new WeakReference<BraintreeClient>(null));
        sut.start();
        sut.uncaughtException(mock(Thread.class), new Exception("error"));

        assertSame(defaultExceptionHandler, Thread.getDefaultUncaughtExceptionHandler());
    }

    @Test
    public void uncaughtExceptionHandler_whenCauseUnknown_forwardsInvocationToDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler);

        Thread thread = mock(Thread.class);
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("test", "test", "test", 1) });

        CrashReporter sut = new CrashReporter(braintreeClient);
        sut.start();
        sut.uncaughtException(thread, exception);

        verify(defaultExceptionHandler).uncaughtException(thread, exception);
    }

    @Test
    public void uncaughtExceptionHandler_whenBraintreeInStackTrace_reportsCrashAndForwardsInvocationToDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler);

        Thread thread = mock(Thread.class);
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.braintreepayments.api.CrashReporting", "test", "test", 1) });

        CrashReporter sut = new CrashReporter(braintreeClient);
        sut.start();
        sut.uncaughtException(thread, exception);

        verify(braintreeClient).reportCrash();
        verify(defaultExceptionHandler).uncaughtException(thread, exception);
    }

    @Test
    public void uncaughtExceptionHandler_whenPayPalInStackTrace_reportsCrashAndForwardsInvocationToDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler);

        Thread thread = mock(Thread.class);
        Exception exception = new Exception();
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.paypal.CrashReporting", "test", "test", 1) });

        CrashReporter sut = new CrashReporter(braintreeClient);
        sut.start();
        sut.uncaughtException(thread, exception);

        verify(braintreeClient).reportCrash();
        verify(defaultExceptionHandler).uncaughtException(thread, exception);
    }
}
