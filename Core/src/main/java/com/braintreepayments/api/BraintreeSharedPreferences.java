package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;

class BraintreeSharedPreferences {

    static SharedPreferences getSharedPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);
    }
}
