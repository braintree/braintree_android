package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Mandate type for the SEPA Debit request.
 */
public enum SEPADebitMandateType {
    RECURRENT("RECURRENT"),
    ONE_OFF("ONE_OFF")
    ;

    private final String mandateType;

    SEPADebitMandateType(final String mandateType) {
        this.mandateType = mandateType;
    }

    static SEPADebitMandateType fromString(String mandateType) {
        switch (mandateType) {
            case "RECURRENT":
                return SEPADebitMandateType.RECURRENT;
            case "ONE_OFF":
                return SEPADebitMandateType.ONE_OFF;
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