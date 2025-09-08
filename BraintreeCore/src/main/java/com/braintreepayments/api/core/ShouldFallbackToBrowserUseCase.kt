package com.braintreepayments.api.core

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShouldFallbackToBrowserUseCase(
    private val merchantRepository: MerchantRepository
) {

    companion object {
        private const val PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile"
    }

    enum class Result {
        FALLBACK, APP_SWITCH
    }

    operator fun invoke(
        uri: Uri?,
        appPackage: String = PAYPAL_APP_PACKAGE
    ): Result {
        if (uri == null) {
            return Result.APP_SWITCH
        }

        val context = merchantRepository.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val resolvedActivity = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val wouldOpenInTargetApp = resolvedActivity?.activityInfo?.packageName == appPackage

        return if (!wouldOpenInTargetApp) {
            Result.FALLBACK
        } else {
            Result.APP_SWITCH
        }
    }
}