package com.braintreepayments.api.paypal

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.MerchantRepository

/**
 * Use case that determines whether the PayPal app-switch link will open in the PayPal app
 * or in a browser.
 *
 * Detects if a user unchecks the "Open supported links" checkbox in the Android OS settings for the PayPal app.
 * If this setting is unchecked, this use case will return [Result.APP], otherwise [Result.BROWSER] will be returned.
 */
internal class GetPaypalLaunchTypeUseCase(
    private val merchantRepository: MerchantRepository
) {

    /**
     * Represents the possible launch types for PayPal payment flows.
     */
    enum class Result {
        /** 
         * The payment flow will be launched in a web browser
         */
        BROWSER,

        /** 
         * The payment flow will be launched in the PayPal app 
         */
        APP
    }

    /** 
     * Detects if users disable the "Open supported links" setting in the Paypal app.
     * @param uri The app-switch URI to be launched
     * @return [Result.APP] if the PayPal app can handle the URI, [Result.BROWSER] otherwise.
     */
    operator fun invoke(
        uri: Uri
    ): Result {
        val context = merchantRepository.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val resolvedActivity = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        val opensInTargetApp =
            resolvedActivity?.activityInfo?.packageName == DeviceInspector.PAYPAL_APP_PACKAGE

        return if (opensInTargetApp) {
            Result.APP
        } else {
            Result.BROWSER
        }
    }
}
