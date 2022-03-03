package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Implement this interface to receive SEPA Debit result notifications.
 */
public interface SEPADebitListener {

    /**
     * Called when SEPA Debit tokenization is complete without error.
     * @param sepaDebitNonce SPEA Debit tokenization result
     */
    void onSEPADebitSuccess(@NonNull SEPADebitNonce sepaDebitNonce);

    /**
     * Called when SEPA Debit tokenization has failed with an error.
     * @param error explains reason for SEPA Debit failure.
     */
    void onSEPADebitFailure(@NonNull Exception error);
}
