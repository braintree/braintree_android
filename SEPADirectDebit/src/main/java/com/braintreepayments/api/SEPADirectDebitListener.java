package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Implement this interface to receive SEPA Direct Debit result notifications.
 */
public interface SEPADirectDebitListener {

    /**
     * Called when SEPA Direct Debit tokenization is complete without error.
     * @param sepaDirectDebitNonce SEPA Direct Debit tokenization result
     */
    void onSEPADirectDebitSuccess(@NonNull SEPADirectDebitNonce sepaDirectDebitNonce);

    /**
     * Called when SEPA Direct Debit tokenization has failed with an error.
     * @param error explains reason for SEPA Direct Debit failure.
     */
    void onSEPADirectDebitFailure(@NonNull Exception error);
}
