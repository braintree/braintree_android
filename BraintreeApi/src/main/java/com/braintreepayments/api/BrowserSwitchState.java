package com.braintreepayments.api;

/**
 * Represents the state of a browser switch.
 */
class BrowserSwitchState {

    /**
     * Represents the current browser switch state.
     * If this is null, there is no current browser switch.
     * Otherwise, this is the request code for the browser switch.
     */
    private Integer mRequestCode;

    public BrowserSwitchState() {
        mRequestCode = null;
    }

    /**
     * Sets the browser switch state to a switch in progress.
     * @param requestCode
     */
    public void begin(int requestCode) {
        mRequestCode = requestCode;
    }

    /**
     * If we have an ongoing browser switch, then it must have been cancelled
     * and send a callback to {@link BraintreeFragment}.
     */
    public void checkForCancel(BraintreeFragment fragment) {
        if (mRequestCode != null) {
            fragment.postCancelCallback(mRequestCode);
            end(mRequestCode);
        }
    }

    /**
     * Sets the browser switch state to no current switch.
     */
    public void end(int requestCode) {
        if(mRequestCode == requestCode) {
            mRequestCode = null;
        }
    }
}
