package com.braintreepayments.api;

import android.content.Context;

import com.braintreepayments.api.internal.AppHelper;

public class DeviceCapabilities {

    private static final String PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile";
    private static final String VENMO_APP_PACKAGE = "com.venmo";

    private DeviceCapabilities() {}

    public static boolean isPayPalInstalled(Context context) {
        return AppHelper.isAppInstalled(context, PAYPAL_APP_PACKAGE);
    }

    public static boolean isVenmoInstalled(Context context) {
        return AppHelper.isAppInstalled(context, VENMO_APP_PACKAGE);
    }
}
