package com.braintreepayments.api;

import com.visa.checkout.VisaPaymentSummary;

/**
 * Callback for receiving result of
 * {@link VisaCheckoutClient#tokenize(VisaPaymentSummary, VisaCheckoutTokenizeCallback)}.
 */
public interface VisaCheckoutTokenizeCallback {

    /**
     * @param paymentMethodNonce {@link PaymentMethodNonce}
     * @param error an exception that occurred while tokenizing a Visa payment method
     */
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
