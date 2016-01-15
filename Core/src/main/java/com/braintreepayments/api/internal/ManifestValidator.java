package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class ManifestValidator {

    public static boolean isActivityDeclaredInAndroidManifest(Context context, Class klass) {
        try {
            PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            ActivityInfo[] activities = packageInfo.activities;
            if (activities != null) {
                for (ActivityInfo activityInfo : activities) {
                    if (activityInfo.name.equals(klass.getName())) {
                        return true;
                    }
                }
            }
        } catch (NameNotFoundException ignored) {}

        return false;
    }
}
