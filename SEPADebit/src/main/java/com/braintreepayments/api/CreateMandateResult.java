package com.braintreepayments.api;

class CreateMandateResult {

    private final String approvalUrl;
    private final String ibanLastFour;
    private final String customerId;
    private final String bankReferenceToken;
    private final SEPADebitMandateType mandateType;

    CreateMandateResult(String approvalUrl, String ibanLastFour, String customerId, String bankReferenceToken, String mandateType) {
       this.approvalUrl = approvalUrl;
       this.ibanLastFour = ibanLastFour;
       this.customerId = customerId;
       this.bankReferenceToken = bankReferenceToken;
       this.mandateType = SEPADebitMandateType.fromString(mandateType);
    }

    public String getApprovalUrl() {
        return approvalUrl;
    }

    public String getIbanLastFour() {
        return ibanLastFour;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getBankReferenceToken() {
        return bankReferenceToken;
    }

    public SEPADebitMandateType getMandateType() {
        return mandateType;
    }

}
