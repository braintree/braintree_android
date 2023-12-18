package com.braintreepayments.api

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable

/**
 * Responsible for launching the Venmo app to authenticate users
 */
interface ComposeVenmoLauncher {


}

@Composable
fun rememberVenmoLauncher(callback: VenmoLauncherCallback) : VenmoLauncher {
    val activityLauncher = rememberLauncherForActivityResult(VenmoActivityResultContract()) {
        callback.onVenmoPaymentAuthResult(it)
    }

    return VenmoLauncher(activityLauncher)
}
