package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.braintreepayments.api.DeviceCapabilities;

import org.json.JSONException;
import org.json.JSONObject;

public class AnalyticsEvent {

    private static final String SESSION_ID_KEY = "sessionId";
    private static final String DEVICE_NETWORK_TYPE_KEY = "deviceNetworkType";
    private static final String USER_INTERFACE_ORIENTATION_KEY = "userInterfaceOrientation";
    private static final String MERCHANT_APP_VERSION_KEY = "merchantAppVersion";
    private static final String PAYPAL_INSTALLED_KEY = "paypalInstalled";
    private static final String VENMO_INSTALLED_KEY = "venmoInstalled";
    private static final String INTEGRATION_TYPE_KEY = "integrationType";
    private static final String DROP_IN_VERSION_KEY = "dropinVersion";

    int id;
    String event;
    long timestamp;
    JSONObject metadata = new JSONObject();

    public AnalyticsEvent() {}

    public AnalyticsEvent(Context context, String sessionId, String integration, String event) {
        this.event = "android." + event;
        this.timestamp = System.currentTimeMillis();
        try {
            metadata.put(SESSION_ID_KEY, sessionId)
                    .put(INTEGRATION_TYPE_KEY, integration)
                    .put(DEVICE_NETWORK_TYPE_KEY, getNetworkType(context))
                    .put(USER_INTERFACE_ORIENTATION_KEY, getUserOrientation(context))
                    .put(MERCHANT_APP_VERSION_KEY, getAppVersion(context))
                    .put(PAYPAL_INSTALLED_KEY, DeviceCapabilities.isPayPalInstalled(context))
                    .put(VENMO_INSTALLED_KEY, DeviceCapabilities.isVenmoInstalled(context))
                    .put(DROP_IN_VERSION_KEY, getDropInVersion());
        } catch (JSONException ignored) {}
    }

    private String getNetworkType(Context context) {
        String networkType = null;
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            networkType = networkInfo.getTypeName();
        }
        if (networkType == null) {
            networkType = "none";
        }
        return networkType;
    }

    private String getUserOrientation(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return "Portrait";
            case Configuration.ORIENTATION_LANDSCAPE:
                return "Landscape";
            default:
                return "Unknown";
        }
    }

    private String getAppVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return "VersionUnknown";
        }
    }

    /**
     * Gets the current Drop-in version or null.
     *
     * @return string representation of the current Drop-in version, or null if
     * Drop-in is unavailable
     */
    private static String getDropInVersion() {
        return ClassHelper.getFieldValue(
                "com.braintreepayments.api.dropin.BuildConfig",
                "VERSION_NAME"
        );
    }
}
