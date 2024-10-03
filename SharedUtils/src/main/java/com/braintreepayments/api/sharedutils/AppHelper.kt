package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AppHelper {

    fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return context
            .queryIntentActivities(intent)
            .isNotEmpty()
    }

    fun isIntentAvailableForPackageName(context: Context, intent: Intent, packageName: String): Boolean {
        return context
            .queryIntentActivities(intent)
            .any { it.activityInfo.packageName == packageName }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        return try {
            packageManager.getApplicationInfo(packageName, NO_FLAGS)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun Context.queryIntentActivities(intent: Intent): List<ResolveInfo> {
        return packageManager.queryIntentActivities(intent, NO_FLAGS)
    }

    companion object {
        private const val NO_FLAGS = 0
    }
}
