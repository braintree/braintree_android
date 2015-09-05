package com.braintreepayments.api.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Additional processing options for creating a {@link com.braintreepayments.api.models.PaymentMethod}
 * in the Braintree gateway.
 */
public class PaymentMethodOptions implements Serializable {
    
    @SerializedName("validate") private boolean mValidate;
    @SerializedName("storeInVault") private boolean mStoreInVault;

    public void setValidate(boolean validate) {
        mValidate = validate;
    }

    public void setStoreInVault(boolean storeInVault) {
        mStoreInVault = storeInVault;
    }
}
