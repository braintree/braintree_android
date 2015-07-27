package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.PaymentMethod;

/**
 * Communicates {@link PaymentMethod} from a HTTP request on the main thread.
 * One and only one method will be invoked in response to a request.
 */
public interface PaymentMethodResponseCallback {

    /**
     * @param paymentMethod parsed {@link PaymentMethod} from the HTTP request.
     */
    void success(PaymentMethod paymentMethod);

    /**
     * @param exception error that caused the request to fail.
     */
    void failure(Exception exception);
}
