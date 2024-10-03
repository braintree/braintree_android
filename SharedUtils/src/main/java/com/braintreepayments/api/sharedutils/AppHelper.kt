package com.braintreepayments.api.sharedutils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ResolveInfo
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_NONE
import android.os.Build
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
            .filter { it.activityInfo.packageName == packageName }
            .any { areAppLinksVerified(context, it.activityInfo.packageName) }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        return try {
            packageManager.getApplicationInfo(packageName, NO_FLAGS)
            return true
        } catch (e: NameNotFoundException) {
            false
        }
    }

    fun areAppLinksVerified(context: Context, packageName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        val domainVerificationUserState = try {
            context
                .getSystemService(DomainVerificationManager::class.java)
                .getDomainVerificationUserState(packageName) ?: return false
        } catch (exception: Exception) {
            return false
        }

        if (!domainVerificationUserState.isLinkHandlingAllowed) {
            return false
        }

        return domainVerificationUserState
            .hostToStateMap
            .map { it.value }
            .all { it != DOMAIN_STATE_NONE }
    }

    private fun Context.queryIntentActivities(intent: Intent): List<ResolveInfo> {
        return packageManager.queryIntentActivities(intent, NO_FLAGS)
    }

    companion object {
        private const val NO_FLAGS = 0
    }
}
