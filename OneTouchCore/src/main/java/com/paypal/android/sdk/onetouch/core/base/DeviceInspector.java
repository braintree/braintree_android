package com.paypal.android.sdk.onetouch.core.base;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

public class DeviceInspector {

    /**
     * @return {@code true} if any network is connected or connecting, {@code false} otherwise.
     */
    public static boolean isNetworkAvailable(Context context) {
        int networkCount = 0;
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null == connectivityManager) {
            return true;
        } else if (null == connectivityManager.getAllNetworkInfo()) {
            return true;
        }

        // Iterate over all of the available networks
        for (NetworkInfo info : connectivityManager.getAllNetworkInfo()) {
            if (info.isConnectedOrConnecting()) {
                networkCount += 1;
            }
        }

        return networkCount > 0;
    }

    public static int getPhoneType(Context context) {
        return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                .getPhoneType();
    }

    public static String getApplicationInfoName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getPackageInfo(context.getPackageName(), 0)
                    .applicationInfo
                    .loadLabel(packageManager)
                    .toString();
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public static String getSimOperatorName(Context context) {
        try {
            return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                    .getSimOperatorName();
        } catch (SecurityException e) {
            return null;
        }
    }

    public static String getDeviceName() {
        String manufacturer = android.os.Build.MANUFACTURER;
        String model = android.os.Build.MODEL;
        if (manufacturer.equalsIgnoreCase("unknown") || model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer + " " + model;
        }
    }

    public static String getOs() {
        return "Android " + Build.VERSION.RELEASE;
    }
}
