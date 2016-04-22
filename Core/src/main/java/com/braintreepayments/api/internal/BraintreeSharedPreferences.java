package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.SharedPreferences;

public class BraintreeSharedPreferences {

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);
    }
}
