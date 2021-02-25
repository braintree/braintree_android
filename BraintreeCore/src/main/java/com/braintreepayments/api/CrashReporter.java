package com.braintreepayments.api;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.annotation.Retention;
import java.lang.ref.WeakReference;

import static java.lang.annotation.RetentionPolicy.SOURCE;

class CrashReporter implements Thread.UncaughtExceptionHandler {

    @Retention(SOURCE)
    @IntDef({CAUSE_UNKNOWN, CAUSE_RELATED_TO_PAYPAL, CAUSE_RELATED_TO_BRAINTREE})
    @interface Cause {}
    static final int CAUSE_UNKNOWN = 0;
    static final int CAUSE_RELATED_TO_PAYPAL = 1;
    static final int CAUSE_RELATED_TO_BRAINTREE = 2;

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

    private final WeakReference<BraintreeClient> braintreeClientRef;

    CrashReporter(BraintreeClient braintreeClient) {
        this(new WeakReference<>(braintreeClient));
    }

    @VisibleForTesting
    CrashReporter(WeakReference<BraintreeClient> braintreeClientRef) {
        this.braintreeClientRef = braintreeClientRef;
    }

    private void registerExceptionHandler(UncaughtExceptionHandler handler) {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    private void restoreDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler);
        defaultExceptionHandler = null;
    }

    @Cause
    private int determineExceptionCause(Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));

        @Cause int result;
        if (stringWriter.toString().contains("com.braintreepayments")) {
            result = CAUSE_RELATED_TO_BRAINTREE;
        } else if (stringWriter.toString().contains("com.paypal")) {
            result = CAUSE_RELATED_TO_PAYPAL;
        } else {
            result = CAUSE_UNKNOWN;
        }
        return result;
    }

    private void handleExceptionWithDefaultBehavior(Thread thread, Throwable ex) {
        if (defaultExceptionHandler != null) {
            defaultExceptionHandler.uncaughtException(thread, ex);
        }
    }

    void start() {
        registerExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable exception) {
        BraintreeClient braintreeClient = braintreeClientRef.get();
        if (braintreeClient == null) {
            handleExceptionWithDefaultBehavior(thread, exception);
            restoreDefaultExceptionHandler();
            return;
        }

        @CrashReporter.Cause int result = determineExceptionCause(exception);
        if (result == CAUSE_RELATED_TO_BRAINTREE || result == CAUSE_RELATED_TO_PAYPAL) {
            braintreeClient.reportCrash();
        }
        handleExceptionWithDefaultBehavior(thread, exception);
    }
}
