package com.braintreepayments.api.interfaces;

import android.support.annotation.MainThread;

/**
 * Callback used by {@link com.braintreepayments.api.BraintreeFragment} to queue responses to various
 * async operations.
 */
public interface QueuedCallback {

    /**
     * @return {@code true} if the run method should be called, {@code false} otherwise.
     */
    boolean shouldRun();

    /**
     * Method to execute arbitrary code on main thread.
     */
    @MainThread
    void run();
}
