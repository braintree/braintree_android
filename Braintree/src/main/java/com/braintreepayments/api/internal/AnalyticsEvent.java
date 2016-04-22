package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;

import com.braintreepayments.api.Venmo;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

public class AnalyticsEvent {

    int id;
    String event;
    long timestamp;
    String sessionId;
    String networkType;
    String interfaceOrientation;
    String merchantAppVersion;
    boolean paypalInstalled;
    boolean venmoInstalled;

    public AnalyticsEvent(Context context, String sessionId, String integration, String event) {
        this.event = "android." + integration + "." + event;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis() / 1000;
        this.networkType = getNetworkType(context);
        this.interfaceOrientation = getUserOrientation(context);
        this.merchantAppVersion = getAppVersion(context);
        this.paypalInstalled = PayPalOneTouchCore.isWalletAppInstalled(context);
        this.venmoInstalled = Venmo.isVenmoInstalled(context);
    }

    AnalyticsEvent() {}

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
        if (connectivityManager.getActiveNetworkInfo() != null) {
            networkType = connectivityManager.getActiveNetworkInfo().getTypeName();
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
