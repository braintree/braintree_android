package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Local payment result information.
 */
public class LocalPaymentResult {

    private final LocalPaymentRequest request;
    private final String approvalUrl;
    private final String paymentId;

    LocalPaymentResult(LocalPaymentRequest request, String approvalUrl, String paymentId) {
        this.request = request;
        this.approvalUrl = approvalUrl;
        this.paymentId = paymentId;
    }

    /**
     * @return The original request used to create the local payment transaction.
     */
    @NonNull
    public LocalPaymentRequest getRequest() {
        return request;
    }

    /**
     * @return The URL used for payment approval.
     */
    @NonNull
    public String getApprovalUrl() {
        return approvalUrl;
    }

    /**
     * @return The ID of the local payment after creation.
     */
    @NonNull
    public String getPaymentId() {
        return paymentId;
    }
}
