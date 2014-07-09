package com.braintreepayments.api.models;

import java.io.Serializable;

/**
 * Additional processing options for creating a {@link com.braintreepayments.api.models.PaymentMethod}
 * in the Braintree gateway.
 */
public class PaymentMethodOptions implements Serializable {

    private boolean validate;

    public void setValidate(boolean validate) {
        this.validate = validate;
    }
}
