package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ManifestValidator {

    fun <T> getActivityInfo(context: Context, klass: Class<T>): ActivityInfo? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            val activities = packageInfo.activities
            activities?.firstOrNull { it.name == klass.name }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
