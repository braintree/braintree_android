package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Mandate type for the SEPA Direct Debit request.
 */
public enum SEPADirectDebitMandateType {
    RECURRENT("RECURRENT"),
    ONE_OFF("ONE_OFF")
    ;

    private final String mandateType;

    SEPADirectDebitMandateType(final String mandateType) {
        this.mandateType = mandateType;
    }

    static SEPADirectDebitMandateType fromString(String mandateType) {
        switch (mandateType) {
            case "RECURRENT":
                return SEPADirectDebitMandateType.RECURRENT;
            case "ONE_OFF":
                return SEPADirectDebitMandateType.ONE_OFF;
            default:
                return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return mandateType;
    }
}