package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.internal.ClassHelper;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.res.Configuration.ORIENTATION_UNDEFINED;

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
    DeviceInspector deviceInspector;

    public AnalyticsEvent() {}

    public AnalyticsEvent(Context context, String sessionId, String integration, String event) {
        this(context, sessionId, integration, event, new DeviceInspector());
    }

    @VisibleForTesting
    AnalyticsEvent(Context context, String sessionId, String integration, String event, DeviceInspector deviceInspector) {
        this.event = "android." + event;
        this.timestamp = System.currentTimeMillis();
        this.deviceInspector = deviceInspector;
        try {
            metadata.put(SESSION_ID_KEY, sessionId)
                    .put(INTEGRATION_TYPE_KEY, integration)
                    .put(DEVICE_NETWORK_TYPE_KEY, getNetworkType(context))
                    .put(USER_INTERFACE_ORIENTATION_KEY, getUserOrientation(context))
                    .put(MERCHANT_APP_VERSION_KEY, getAppVersion(context))
                    .put(PAYPAL_INSTALLED_KEY, isPayPalInstalled(context))
                    .put(VENMO_INSTALLED_KEY, isVenmoInstalled(context))
                    .put(DROP_IN_VERSION_KEY, getDropInVersion());
        } catch (JSONException ignored) {}
    }

    private String getNetworkType(Context context) {
        String networkType = null;
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                networkType = networkInfo.getTypeName();
            }
            if (networkType == null) {
                networkType = "none";
            }
        }
        return networkType;
    }

    private String getUserOrientation(Context context) {
        int orientation = ORIENTATION_UNDEFINED;
        if (context != null) {
            orientation = context.getResources().getConfiguration().orientation;
        }

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
        String result = "VersionUnknown";
        if (context != null) {
            try {
                result = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (NameNotFoundException ignored) { /* do nothing */ }
        }
        return result;
    }

    private boolean isPayPalInstalled(Context context) {
        return deviceInspector.isPayPalInstalled(context);
    }

    private boolean isVenmoInstalled(Context context) {
        return deviceInspector.isVenmoInstalled(context);
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
