package com.braintreepayments.api;

public class LocalPaymentTransaction {

    private final LocalPaymentRequest request;
    private final String approvalUrl;
    private final String paymentId;

    LocalPaymentTransaction(LocalPaymentRequest request, String approvalUrl, String paymentId) {
        this.request = request;
        this.approvalUrl = approvalUrl;
        this.paymentId = paymentId;
    }

    /**
     * @return The original request used to create the local payment transaction.
     */
    public LocalPaymentRequest getRequest() {
        return request;
    }

    /**
     * @return The URL used for payment approval.
     */
    public String getApprovalUrl() {
        return approvalUrl;
    }

    /**
     * @return The ID of the local payment after creation.
     */
    public String getPaymentId() {
        return paymentId;
    }
}
