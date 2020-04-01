package com.braintreepayments.demo.models;

import com.google.gson.annotations.SerializedName;

public class PaymentMethodToken {

    @SerializedName("token")
    private String mToken;

    public String getToken() {
        return mToken;
    }
}
