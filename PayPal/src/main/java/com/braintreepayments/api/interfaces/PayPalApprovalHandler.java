package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.PayPalApiRequest;

/**
 * Interface that could be extended for custom authentication and authorization of PayPal.
 */
public interface PayPalApprovalHandler {

    /**
     * Handle approval request for PayPal and carry out custom authentication and authorization.
     * On successful completion, {@link PayPalApprovalHandler}`s onComplete() is called.
     * On cancel, {@link PayPalApprovalHandler}'s onCancel() is called.
     *
     * @param request PayPal request object.
     * @param paypalApprovalCallback callback method to handle response.
     */
    void handleApproval(PayPalApiRequest request, PayPalApprovalCallback paypalApprovalCallback);
}
