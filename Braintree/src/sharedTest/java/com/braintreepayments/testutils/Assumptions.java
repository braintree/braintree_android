package com.braintreepayments.testutils;

import android.os.Build;

import static org.junit.Assume.assumeTrue;

public class Assumptions {
    public static void assumeDeviceCanConnectToBraintreeApi() {
        assumeTrue("braintree-api.com SSL connection does not support API < 21 (Lollipop)",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }
}
