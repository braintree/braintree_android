package com.braintreepayments.api;

class CreateMandateResult {

    private final String approvalUrl;
    private final String ibanLastFour;
    private final String customerId;
    private final String bankReferenceToken;
    private final SEPADirectDebitMandateType mandateType;

    CreateMandateResult(String approvalUrl, String ibanLastFour, String customerId, String bankReferenceToken, String mandateType) {
       this.approvalUrl = approvalUrl;
       this.ibanLastFour = ibanLastFour;
       this.customerId = customerId;
       this.bankReferenceToken = bankReferenceToken;
       this.mandateType = SEPADirectDebitMandateType.fromString(mandateType);
    }

    String getApprovalUrl() {
        return approvalUrl;
    }

    String getIbanLastFour() {
        return ibanLastFour;
    }

    String getCustomerId() {
        return customerId;
    }

    String getBankReferenceToken() {
        return bankReferenceToken;
    }

    SEPADirectDebitMandateType getMandateType() {
        return mandateType;
    }

}
