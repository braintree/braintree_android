package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class VenmoInstalledContextFactory {

    public static Context venmoInstalledContext(boolean installed) {
        return venmoInstalledContext(installed, null);
    }

    public static Context venmoInstalledContext(boolean installed, Context context) {
        if (context == null) {
            context = mock(Context.class);
        } else {
            context = spy(context);
        }

        PackageManager pm = installed ? mockPackageManager() : mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(pm);

        return context;
    }

    private static PackageManager mockPackageManager() {
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "com.venmo";
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        final PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(resolveInfo));
        return packageManager;
    }
}

