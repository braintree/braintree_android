package com.braintreepayments.api;

import android.os.Parcelable;

public interface PaymentMethodNonce extends Parcelable {
    /**
     * @return The nonce generated for this payment method by the Braintree gateway. The nonce will
     *          represent this PaymentMethod for the purposes of creating transactions and other monetary
     *          actions.
     */
    String getNonce();

    /**
     * @return The description of this PaymentMethod for displaying to a customer, e.g. 'Visa ending in...'
     */
    String getDescription();

    /**
     * @return {@code true} if this payment method is the default for the current customer, {@code false} otherwise
     */
    boolean isDefault();

    /**
     * @return The type of this PaymentMethod for displaying to a customer, e.g. 'Visa'. Can be used
     *          for displaying appropriate logos, etc.
     */
    String getTypeLabel();
}
