package com.braintreepayments.demo.models;

import com.google.gson.annotations.SerializedName;

public class PayPalUAT {
    @SerializedName("id_token")
    private String uat;

    public String getUAT() { return uat; }
}
