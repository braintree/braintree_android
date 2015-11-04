package com.paypal.android.sdk.onetouch.core.base;

import android.os.Build;

public class DeviceInspector {

    public String getDeviceName() {
        String manufacturer = android.os.Build.MANUFACTURER;
        String model = android.os.Build.MODEL;
        if (manufacturer.equalsIgnoreCase("unknown") || model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer + " " + model;
        }
    }

    public String getOs() {
        return "Android " + Build.VERSION.RELEASE;
    }
}
