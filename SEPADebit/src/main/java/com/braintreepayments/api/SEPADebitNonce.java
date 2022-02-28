package com.braintreepayments.api;

import android.os.Parcel;

import androidx.annotation.NonNull;

/**
 * {@link PaymentMethodNonce} representing a SEPA Debit payment.
 *
 * @see PaymentMethodNonce
 */
public class SEPADebitNonce extends PaymentMethodNonce {

    private static final String TYPE_KEY = "type";
    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";
    private static final String DESCRIPTION_KEY = "description";
    private static final String CONSUMED_KEY = "consumed";
    private static final String DETAILS_KEY = "details";
    private static final String IBAN_LAST_FOUR_KEY = "ibanLastChars";
    private static final String CUSTOMER_ID_KEY = "customerId";
    private static final String MANDATE_TYPE_KEY = "mandateType";

    private final String ibanLastChars;
    private final String customerId;
    private final SEPADebitMandateType mandateType;

    protected SEPADebitNonce(Parcel in) {
        super(in);
    }
}
