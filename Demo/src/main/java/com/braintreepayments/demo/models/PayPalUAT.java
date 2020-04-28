package com.braintreepayments.demo.models;

import com.google.gson.annotations.SerializedName;

public class PayPalUAT {
    @SerializedName("universal_access_token")
    private String mUAT;

    public String getUAT() { return mUAT; }
}
