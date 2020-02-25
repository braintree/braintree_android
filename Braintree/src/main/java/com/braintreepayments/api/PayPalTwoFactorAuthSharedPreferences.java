package com.braintreepayments.api;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.util.Base64;

import com.braintreepayments.api.internal.BraintreeSharedPreferences;
import com.braintreepayments.api.models.PayPalAccountNonce;

class PayPalTwoFactorAuthSharedPreferences {
    private static final String PAYPAL_TWO_FACTOR_AUTH_REQUEST_KEY = "com.braintreepayments.api.PayPalTwoFactorAuth.PAYPAL_TWO_FACTOR_AUTH_REQUEST_KEY";

    static void persistPayPalAccountNonce(BraintreeFragment fragment, PayPalAccountNonce payPalAccountNonce) {
        Parcel parcel = Parcel.obtain();
        payPalAccountNonce.writeToParcel(parcel, 0);
        BraintreeSharedPreferences.getSharedPreferences(fragment.getApplicationContext()).edit()
                .putString(PAYPAL_TWO_FACTOR_AUTH_REQUEST_KEY, Base64.encodeToString(parcel.marshall(), 0))
                .apply();
    }

    static PayPalAccountNonce getPersistedPayPalAccountNonce(BraintreeFragment fragment) {
        SharedPreferences prefs = BraintreeSharedPreferences.getSharedPreferences(fragment.getApplicationContext());
        try {
            byte[] requestBytes = Base64.decode(prefs.getString(PAYPAL_TWO_FACTOR_AUTH_REQUEST_KEY, ""), 0);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(requestBytes, 0, requestBytes.length);
            parcel.setDataPosition(0);
            return PayPalAccountNonce.CREATOR.createFromParcel(parcel);
        } catch (Exception ignored) {
        } finally {
            prefs.edit()
                    .remove(PAYPAL_TWO_FACTOR_AUTH_REQUEST_KEY)
                    .apply();
        }

        return null;
    }
}
