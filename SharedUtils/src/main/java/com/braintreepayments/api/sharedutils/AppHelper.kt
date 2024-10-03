package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AppHelper {

    fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return context
            .packageManager
            .queryIntentActivities(intent, 0)
            .isNotEmpty()
    }

    fun isIntentAvailableForPackageName(context: Context, intent: Intent, packageName: String): Boolean {
        return context
            .packageManager
            .queryIntentActivities(intent, 0)
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

    companion object {
        private const val NO_FLAGS = 0
    }
}
