package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.List;

public class AppHelper {

    public static boolean isIntentAvailable(Context context, Intent intent) {
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
        return activities != null && activities.size() == 1;
    }
}
