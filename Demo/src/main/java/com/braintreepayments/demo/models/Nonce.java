package com.braintreepayments.demo.models;

import com.google.gson.annotations.SerializedName;

public class Nonce {

    @SerializedName("nonce")
    private String mNonce;

    public String getNonce() {
        return mNonce;
    }
}
