package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.support.annotation.Nullable;

public class ManifestValidator {

    public static boolean isActivityDeclaredInAndroidManifest(Context context, Class klass) {
        return getActivityInfo(context, klass) != null;
    }

    @Nullable
    public static ActivityInfo getActivityInfo(Context context, Class klass) {
        try {
            PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            ActivityInfo[] activities = packageInfo.activities;
            if (activities != null) {
                for (ActivityInfo activityInfo : activities) {
                    if (activityInfo.name.equals(klass.getName())) {
                        return activityInfo;
                    }
                }
            }
        } catch (NameNotFoundException ignored) {}

        return null;
    }
}
