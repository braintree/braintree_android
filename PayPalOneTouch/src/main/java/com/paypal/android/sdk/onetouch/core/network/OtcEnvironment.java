package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;

public class OtcEnvironment {

    public static String getVersion() {
        return BuildConfig.PRODUCT_VERSION;
    }

    public static String getPrefsFile() {
        return "PayPalOTC";
    }

    public static String getUserAgent() {
        return String.format("PayPalSDK/%s %s (%s; %s; %s)", BuildConfig.PRODUCT_NAME, BuildConfig.PRODUCT_VERSION,
                DeviceInspector.getOs(), DeviceInspector.getDeviceName(), getFeatureString());
    }

    private static String getFeatureString() {
        if (BuildConfig.DEBUG) {
            return "debug;";
        }

        return "";
    }
}
