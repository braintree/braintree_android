package com.braintreepayments.api;

/**
 * @deprecated Future Payments are deprecated
 */
@Deprecated
public class PayPalOverrides {

    /**
     * @deprecated Future Payments are deprecated.
     */
    public static void setFuturePaymentsOverride(boolean useFuturePayments) {
        PayPal.sFuturePaymentsOverride = useFuturePayments;
    }
}
