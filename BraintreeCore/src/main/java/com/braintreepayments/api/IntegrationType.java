package com.braintreepayments.api;

import android.content.Context;

public class IntegrationType {

    public static String get(Context context) {
        try {
            if (Class.forName("com.braintreepayments.api.BraintreePaymentActivity").isInstance(context)) {
                return "dropin";
            }
        } catch (ClassNotFoundException ignored) {}

        try {
            if (Class.forName("com.braintreepayments.api.dropin.DropInActivity").isInstance(context)) {
                return "dropin2";
            }
        } catch (ClassNotFoundException ignored) {}

        return "custom";
    }
}
