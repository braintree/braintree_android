package com.braintreepayments.api;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Mandate type for the SEPA Debit request.
 */
@Retention(RetentionPolicy.SOURCE)
@StringDef({
        SEPADebitMandateType.RECURRENT,
        SEPADebitMandateType.ONE_OFF
})
public @interface SEPADebitMandateType {
    String RECURRENT = "RECURRENT";
    String ONE_OFF = "ONE_OFF";
}
