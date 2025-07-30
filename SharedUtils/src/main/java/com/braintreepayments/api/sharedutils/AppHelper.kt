package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AppHelper {

    companion object {
        const val NO_FLAGS = 0
    }

    fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        val activities = context.packageManager.queryIntentActivities(intent, NO_FLAGS)
        return activities.size == 1
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, NO_FLAGS)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
