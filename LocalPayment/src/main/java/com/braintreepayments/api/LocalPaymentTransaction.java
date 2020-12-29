package com.braintreepayments.api;

import com.braintreepayments.api.models.LocalPaymentRequest;

public class LocalPaymentTransaction {

    private final LocalPaymentRequest request;
    private final String approvalUrl;
    private final String paymentId;

    LocalPaymentTransaction(LocalPaymentRequest request, String approvalUrl, String paymentId) {
        this.request = request;
        this.approvalUrl = approvalUrl;
        this.paymentId = paymentId;
    }

    LocalPaymentRequest getRequest() {
        return request;
    }

    String getApprovalUrl() {
        return approvalUrl;
    }

    String getPaymentId() {
        return paymentId;
    }
}
