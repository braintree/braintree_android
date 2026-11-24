package com.braintreepayments.demo.models;

import com.google.gson.annotations.SerializedName;

public class PaymentMethodToken {

    @SerializedName("token")
    private String token;

    // The merchant server's reason when it couldn't vault the payment method.
    @SerializedName("message")
    private String message;

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }
}
