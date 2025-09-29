package com.braintreepayments.api.paypal

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.MerchantRepository

/**
 * Use case that determines whether the PayPal app can resolve app-switch URIs.
 *
 * Detects if the "Open supported links" setting is checked for the PayPal app.
 * If this setting is unchecked, this use case will return false.
 */
internal class ResolvePayPalUseCase(
    private val merchantRepository: MerchantRepository
) {

    /**
     * Checks if the PayPal app can resolve the app-switch URI.
     * @param uri The app-switch URL to be launched. Defaults to [PAYPAL_APP_SWITCH_URL] if not provided.
     * @return true if the PayPal app resolves the URI, false otherwise.
     */
    operator fun invoke(
        uri: Uri = Uri.parse(PAYPAL_APP_SWITCH_URL)
    ): Boolean {
        val context = merchantRepository.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val resolvedActivity = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        return resolvedActivity?.activityInfo?.packageName == DeviceInspector.PAYPAL_APP_PACKAGE
    }

    companion object {
        const val PAYPAL_APP_SWITCH_URL = "https://www.paypal.com/app-switch-checkout"
    }
}
