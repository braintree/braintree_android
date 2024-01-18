package com.braintreepayments.demo;

import android.content.Context;
import android.content.SharedPreferences;

import com.braintreepayments.api.PayPalPendingRequest;

public class PendingRequestStore {

    static final String PREFERENCES_KEY =
            "PENDING_REQUEST_SHARED_PREFERENCES";

    static final String PAYPAL_PENDING_REQUEST_KEY = "PAYPAL_PENDING_REQUEST";

    private static final PendingRequestStore INSTANCE = new PendingRequestStore();

    static PendingRequestStore getInstance() {
        return INSTANCE;
    }

    public static void putPayPalPendingRequest(Context context, PayPalPendingRequest.Started pendingRequest) {
        put(PAYPAL_PENDING_REQUEST_KEY, pendingRequest.toJsonString(), context);
    }

    public static PayPalPendingRequest.Started getPayPalPendingRequest(Context context) {
        String requestString = get(PAYPAL_PENDING_REQUEST_KEY, context);
        if (requestString != null) {
            return new PayPalPendingRequest.Started(requestString);
        }
        return null;
    }

    public static void clearPayPalPendingRequest(Context context) {
        remove(PAYPAL_PENDING_REQUEST_KEY, context);
    }

    static void put(String key, String value, Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences sharedPreferences =
                applicationContext.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(key, value).apply();
    }

    static String get(String key, Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences sharedPreferences =
                applicationContext.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    static void remove(String key, Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences sharedPreferences =
                applicationContext.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(key).apply();
    }

}
