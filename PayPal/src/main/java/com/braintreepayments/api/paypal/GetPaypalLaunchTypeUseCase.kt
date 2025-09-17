package com.braintreepayments.api.paypal

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.MerchantRepository

/**
 * Use case for determining how a PayPal payment flow should be launched.
 *
 * This class analyzes whether the PayPal app-switch link should open in the PayPal app
 * or in a web browser, based on the PayPal app's
 * capability to handle the app-link.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetPaypalLaunchTypeUseCase(
    private val merchantRepository: MerchantRepository
) {

    /**
     * Represents the possible launch types for PayPal payment flows.
     */
    enum class Result {
        /** The payment flow will be launched in a web browser */
        BROWSER,
        /** The payment flow will be launched in the PayPal app */
        APP
    }

    /**
     * Determines whether the PayPal app-switch URI can be opened
     * by the PayPal app or should open in a browser.
     *
     * This method checks if the PayPal app can resolve the app-switch URI using Android's package manager.
     * Even if the PayPal app declares support for the app-switch URI's path pattern, Android may still route it
     * to the browser if the user has disabled "Open supported links" in PayPal app settings.
     *
     * This method detects users disabling the aforementioned setting
     * and enables those flows to be opened in a browser.
     *
     *
     * @param uri The app-switch URI to be launched for the payment flow
     * @param appPackage The package name of the app to check for (defaults to PayPal app package)
     * @return [Result.APP] if the PayPal app can handle the URI, [Result.BROWSER] otherwise
     */
    operator fun invoke(
        uri: Uri,
        appPackage: String = DeviceInspector.PAYPAL_APP_PACKAGE
    ): Result {
        val context = merchantRepository.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val resolvedActivity = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val opensInTargetApp = resolvedActivity?.activityInfo?.packageName == appPackage

        return if (opensInTargetApp) {
            Result.APP
        } else {
            Result.BROWSER
        }
    }
}
