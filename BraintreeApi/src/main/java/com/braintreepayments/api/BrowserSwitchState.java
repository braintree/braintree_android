package com.braintreepayments.api;

/**
 * Represents the state of a browser switch.
 */
class BrowserSwitchState {
    protected Boolean mBrowserSwitchHasReturned;
    protected int mRequestCode;

    public void begin(int requestCode) {
        mRequestCode = requestCode;
        mBrowserSwitchHasReturned = false;
    }

    public void checkForCancel(BraintreeFragment fragment) {
        if (Boolean.FALSE.equals(mBrowserSwitchHasReturned)) {
            fragment.postCancelCallback(mRequestCode);
            mBrowserSwitchHasReturned = null;
        }
    }

    public int end() {
        mBrowserSwitchHasReturned = true;
        return mRequestCode;
    }
}
