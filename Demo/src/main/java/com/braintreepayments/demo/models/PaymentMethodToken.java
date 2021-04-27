package com.braintreepayments.demo.models;

import com.google.gson.annotations.SerializedName;

public class PaymentMethodToken {

    @SerializedName("token")
    private String token;

    public String getToken() {
        return token;
    }
}
