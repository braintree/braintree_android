package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;

import java.util.List;

public class AppHelper {

    private static final int NO_FLAGS = 0;

    public boolean isIntentAvailable(Context context, Intent intent) {
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
        return activities != null && activities.size() == 1;
    }

    public boolean isAppInstalled(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getApplicationInfo(packageName, NO_FLAGS) != null;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
