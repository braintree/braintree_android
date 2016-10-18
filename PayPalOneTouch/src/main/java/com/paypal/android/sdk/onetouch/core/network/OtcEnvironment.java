package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;

public class OtcEnvironment {

    public static String getPrefsFile() {
        return "PayPalOTC";
    }

    public static String getUserAgent() {
        return String.format("PayPalSDK/PayPalOneTouch-Android %s (%s; %s; %s)", BuildConfig.VERSION_NAME,
                DeviceInspector.getOs(), DeviceInspector.getDeviceName(), getFeatureString());
    }

    private static String getFeatureString() {
        return BuildConfig.DEBUG ? "debug;" : "";
    }
}
