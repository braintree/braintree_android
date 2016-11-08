package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.braintreepayments.api.Venmo;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import org.json.JSONException;
import org.json.JSONObject;

public class AnalyticsEvent {

    int id;
    String event;
    long timestamp;
    JSONObject metadata;

    public AnalyticsEvent(Context context, String sessionId, String integration, String event) {
        this.event = "android." + integration + "." + event;
        this.timestamp = System.currentTimeMillis() / 1000;
        metadata = new JSONObject();
        try {
            metadata.put(AnalyticsIntentService.SESSION_ID_KEY, sessionId)
                    .put(AnalyticsIntentService.DEVICE_NETWORK_TYPE_KEY, getNetworkType(context))
                    .put(AnalyticsIntentService.USER_INTERFACE_ORIENTATION_KEY, getUserOrientation(context))
                    .put(AnalyticsIntentService.MERCHANT_APP_VERSION_KEY, getAppVersion(context))
                    .put(AnalyticsIntentService.PAYPAL_INSTALLED_KEY, PayPalOneTouchCore.isWalletAppInstalled(context))
                    .put(AnalyticsIntentService.VENMO_INSTALLED_KEY, Venmo.isVenmoInstalled(context));
        } catch (JSONException ignored) {}
    }

    public AnalyticsEvent() {
        metadata = new JSONObject();
    }

    public String getIntegrationType() {
        String[] eventSegments = this.event.split("\\.");
        if (eventSegments.length > 1) {
            return eventSegments[1];
        } else {
            return "";
        }
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
}
