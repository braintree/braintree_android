package com.paypal.android.sdk.onetouch.core.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class ThemeManifestValidator {
    private final Context context;

    public ThemeManifestValidator(Context context) {
        this.context = context;
    }

    /**
     * Validates that the theme is translucent
     */
    public void validateTheme(Class<? extends Activity> clazz) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo =
                    packageManager.getPackageInfo(
                            context.getPackageName(), PackageManager.GET_ACTIVITIES);

            ActivityInfo[] activityInfos = packageInfo.activities;
            if (null != activityInfos) {
                for (ActivityInfo activityInfo : activityInfos) {
                    if (activityInfo.name.equals(clazz.getName())
                            && activityInfo.getThemeResource() !=
                            android.R.style.Theme_Translucent_NoTitleBar) {
                        throw new RuntimeException("Theme for " + clazz.getName() + " should be " +
                                "\"@android:style/Theme.Translucent.NoTitleBar\"");
                    }
                }
            }
        } catch (NameNotFoundException e) {
            // Name not found, this will probably cause much bigger problems.
            throw new RuntimeException("Exception loading manifest" + e.getMessage());
        }
    }

}
