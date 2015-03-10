package com.braintreepayments.api.models;

import com.braintreepayments.api.Braintree;

/**
 * The result returned in {@link com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener#onBraintreeSetupFinished(SetupResult)}
 * when setup completes.
 */
public class SetupResult {

    private boolean mSetupSuccessful;
    private String mMessage;
    private Exception mException;
    private Braintree mBraintree;

    /**
     * @param setupSuccessful {@code true} if setup was successful, {@code false} otherwise.
     * @param message the message if an exception occurred.
     * @param exception the exception that occured if setup failed.
     * @param braintree the {@link com.braintreepayments.api.Braintree} instance or {@code null}
     *        if setup failed.
     */
    public SetupResult(boolean setupSuccessful, String message, Exception exception, Braintree braintree) {
        mSetupSuccessful = setupSuccessful;
        mMessage = message;
        mException = exception;
        mBraintree = braintree;
    }

    /**
     * @return {@code true} if setup was successful, {@code false} otherwise. {@link #getBraintree()}
     * will only return an instance of {@link com.braintreepayments.api.Braintree} if setup was
     * successful.
     */
    public boolean isSetupSuccessful() {
        return mSetupSuccessful;
    }

    /**
     * @return the error message if setup was not successful.
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * @return the exception that occurred if setup was not successful.
     */
    public Exception getException() {
        return mException;
    }

    /**
     * @return the instance of a setup {@link com.braintreepayments.api.Braintree}. Will return
     * {@code null} if setup was unsuccessful.
     */
    public Braintree getBraintree() {
        return mBraintree;
    }
}
