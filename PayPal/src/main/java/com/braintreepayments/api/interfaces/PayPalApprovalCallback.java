package com.braintreepayments.api.interfaces;

import android.content.Intent;

/**
 * Interface to handle PayPal Approval callback
 */
public interface PayPalApprovalCallback {

    /**
     * The data from approval callback.
     * @param data returned on complete.
     */
    void onComplete(Intent data);

    /**
     * Called on cancel event.
     */
    void onCancel();
}
