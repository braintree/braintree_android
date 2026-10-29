package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayLaterButtonColor

/**
 * A composable that displays a PayPal Pay Later branded button and handles the PayPal Pay Later flow.
 *
 * PayPal Pay Later is only available with PayPal Checkout. The button sets
 * [PayPalCheckoutRequest.shouldOfferPayLater] to `true` on the provided request when it is clicked.
 *
 * @param style: A [PayLaterButtonColor] that determines the color of the button.
 * @param payPalRequest: A [PayPalCheckoutRequest] that provides the parameters to tokenize a PayPal account.
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
fun PayLaterButton(
    style: PayLaterButtonColor,
    payPalRequest: PayPalCheckoutRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String,
    pendingRequestRepository: PendingRequestRepository = PendingRequestRepository(LocalContext.current, "paylater"),
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    PayPalButtonFlow(
        payPalRequest = payPalRequest,
        authorization = authorization,
        appLinkReturnUrl = appLinkReturnUrl,
        deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme,
        pendingRequestRepository = pendingRequestRepository,
        paypalTokenizeCallback = paypalTokenizeCallback,
        onButtonSelected = { payPalRequest.shouldOfferPayLater = true }
    ) { enabled, onClick ->
        PayLaterButtonView(style = style, enabled = enabled, onClick = onClick)
    }
}