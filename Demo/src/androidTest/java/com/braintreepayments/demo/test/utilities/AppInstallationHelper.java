package com.braintreepayments.demo.test.utilities;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import androidx.test.core.app.ApplicationProvider;

public class AppInstallationHelper {

    public static boolean isAppInstalled(String packageName) {
        PackageManager pm = ApplicationProvider.getApplicationContext().getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
