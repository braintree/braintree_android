package com.braintreepayments.demo.models;

import com.google.gson.annotations.SerializedName;

public class Nonce {

    @SerializedName("nonce")
    private String nonce;

    public String getNonce() {
        return nonce;
    }
}
