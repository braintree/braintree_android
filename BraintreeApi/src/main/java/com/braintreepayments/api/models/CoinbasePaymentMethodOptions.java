package com.braintreepayments.api.models;

import com.braintreepayments.api.annotations.Beta;
import com.google.gson.annotations.SerializedName;

/**
 * Additional processing options for creating a {@link com.braintreepayments.api.models.PaymentMethod}
 * in the Braintree gateway for Coinbase.
 */
@Beta
public class CoinbasePaymentMethodOptions extends PaymentMethodOptions{

    /**
     * This property should only be sent when doing a Coinbase payment.
     */
    @SerializedName("storeInVault") private boolean mStoreInVault;

    public void setStoreInVault(boolean storeInVault) {
        mStoreInVault = storeInVault;
    }
}
