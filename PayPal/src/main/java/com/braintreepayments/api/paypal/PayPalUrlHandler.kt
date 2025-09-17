package com.braintreepayments.api.paypal

import android.net.Uri
import com.braintreepayments.api.core.MerchantRepository

/**
 * Utility class for checking if the PayPal app can handle app-switch URLs.
 */
object PayPalUrlHandler {

    private const val DEFAULT_PAYPAL_APP_SWITCH_URL = "https://www.paypal.com/app-switch-checkout"

    /**
     * Checks if the PayPal app can open the given URL.
     *
     * @param redirectUrl The URL received after calling create_payment_resource or setup_billing_agreement
     * @return true if PayPal app can handle the URL, false otherwise
     */
    @JvmStatic
    @JvmOverloads
    fun canPayPalHandleUrl(
        redirectUrl: String = DEFAULT_PAYPAL_APP_SWITCH_URL,
    ): Boolean {
        val getPaypalLaunchTypeUseCase = GetPaypalLaunchTypeUseCase(MerchantRepository.instance)
        return getPaypalLaunchTypeUseCase(Uri.parse(redirectUrl)) ==
            GetPaypalLaunchTypeUseCase.Result.APP
    }
}
