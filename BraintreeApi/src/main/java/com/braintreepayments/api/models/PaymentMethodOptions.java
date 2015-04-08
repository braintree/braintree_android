package com.braintreepayments.api.models;

import java.io.Serializable;

/**
 * Additional processing options for creating a {@link com.braintreepayments.api.models.PaymentMethod}
 * in the Braintree gateway.
 */
public class PaymentMethodOptions implements Serializable {

    private boolean validate;
    private boolean storeInVault;

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setStoreInVault(boolean storeInVault) {
        this.storeInVault = storeInVault;
    }
}
