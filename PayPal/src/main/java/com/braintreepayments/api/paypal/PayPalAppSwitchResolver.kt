package com.braintreepayments.api.paypal

import android.net.Uri
import com.braintreepayments.api.core.MerchantRepository

/**
 * Determines if the PayPal app can resolve app-switch URLs.
 */
internal object PayPalAppSwitchResolver {

    private const val PAYPAL_APP_SWITCH_URL = "https://www.paypal.com/app-switch-checkout"

    /**
     * @param redirectUrl The URL received after calling create_payment_resource or setup_billing_agreement
     * @return true if the [redirectUrl] can open the PayPal app, false otherwise
     */
    @JvmStatic
    fun canPayPalResolveUrl(
        redirectUrl: String = PAYPAL_APP_SWITCH_URL,
    ): Boolean {
        val getPaypalLaunchTypeUseCase = GetPaypalLaunchTypeUseCase(MerchantRepository.instance)
        return getPaypalLaunchTypeUseCase(Uri.parse(redirectUrl)) ==
            GetPaypalLaunchTypeUseCase.Result.APP
    }
}
