package com.braintreepayments.api;

public class PayPalNativeCheckoutRequest extends PayPalCheckoutRequest {

    /**
     * @param amount The transaction amount in currency units (as * determined by setCurrencyCode).
     *               For example, "1.20" corresponds to one dollar and twenty cents. Amount must be a non-negative
     *               number, may optionally contain exactly 2 decimal places separated by '.'
     *               and is limited to 7 digits before the decimal point.
     *               <p>
     *               This amount may differ slightly from the transaction amount. The exact decline rules
     *               for mismatches between this client-side amount and the final amount in the Transaction
     *               are determined by the gateway.
     **/
    public PayPalNativeCheckoutRequest(String amount) {
        super(amount);
    }
}
