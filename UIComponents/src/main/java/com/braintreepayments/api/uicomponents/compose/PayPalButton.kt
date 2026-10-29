package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics.UI_TYPE_COMPOSE

/**
 * A composable that displays a PayPal branded button.
 * @param style: A [PayPalButtonColor] that determines the color of the button.
 * @param payPalRequest: A [PayPalRequest] that provides the parameters to tokenize a paypal account.
 * @param authorization: An authorization string to use for tokenization.
 * @param appLinkReturnUrl: A [Uri] that sends back control to the host app after PayPal flow completes.
 * @param deepLinkFallbackUrlScheme: Fallback scheme in case [appLinkReturnUrl] doesn't work.
 *
 * The value must be a bare URL scheme, not a full URL. It must:
 * - Contain only alphanumeric characters, hyphens (`-`), and periods (`.`)
 * - Not include `://`
 * - Not include a path, query string, or fragment (no `/`, `?`, or `#`)
 * - Match the scheme declared in your `AndroidManifest.xml`
 *
 * Valid examples: `"com.merchant.app.payments"`, `"merchantapp"`, `"merchant-app.payments"`.
 *
 * Invalid examples: `"com.merchant.app.payments://"` (contains `://`),
 * `"com.merchant.app.payments/callback"` (contains a path),
 * `"com.merchant_app"` (underscore not allowed).
 * @param paypalTokenizeCallback: A [PayPalTokenizeCallback] that handles the result of the tokenization.
 */
@Composable
fun PayPalButton(
    style: PayPalButtonColor,
    payPalRequest: PayPalRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String,
    pendingRequestRepository: PendingRequestRepository = PendingRequestRepository(LocalContext.current, "paypal"),
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    var shouldLogButtonPresentment by rememberSaveable { mutableStateOf(true) }
    val analyticsClient = remember { AnalyticsClient.lazyInstance.value }

    PayPalButtonFlow(
        payPalRequest = payPalRequest,
        authorization = authorization,
        appLinkReturnUrl = appLinkReturnUrl,
        deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme,
        pendingRequestRepository = pendingRequestRepository,
        paypalTokenizeCallback = paypalTokenizeCallback,
        onButtonSelected = { logButtonSelected(analyticsClient) }
    ) { enabled, onClick ->
        PayPalButtonView(style = style, enabled = enabled, onClick = onClick)

        LaunchedEffect(Unit) {
            if (shouldLogButtonPresentment) {
                shouldLogButtonPresentment = false
                logButtonPresented(analyticsClient)
            }
        }
    }
}

private fun logButtonPresented(analyticsClient: AnalyticsClient) {
    analyticsClient.sendEvent(
        UIComponentsAnalytics.PAYPAL_BUTTON_PRESENTED,
        AnalyticsEventParams(uiType = UI_TYPE_COMPOSE)
    )
}

private fun logButtonSelected(analyticsClient: AnalyticsClient) {
    analyticsClient.sendEvent(
        UIComponentsAnalytics.PAYPAL_BUTTON_SELECTED,
        AnalyticsEventParams(uiType = UI_TYPE_COMPOSE)
    )
}