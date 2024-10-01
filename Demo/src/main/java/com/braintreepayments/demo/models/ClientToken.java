package com.braintreepayments.demo.models;

import com.google.gson.annotations.SerializedName;

public class ClientToken {

    @SerializedName("client_token")
    private String clientToken;

    public String getClientToken() {
        return clientToken;
    }
}
