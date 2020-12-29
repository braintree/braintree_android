package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.PreferredPaymentMethodsResult;

/**
 * Interface for PreferredPaymentMethods callbacks.
 * This interface is currently in beta and may change in future releases.
 */
public interface PreferredPaymentMethodsCallback extends BraintreeListener {

    /**
     * Called when the preferred payment methods result is available.
     *
     * @param preferredPaymentMethodsResult Information about which payment methods should be preferred in your UI.
     */
    void onResult(PreferredPaymentMethodsResult preferredPaymentMethodsResult);
}
