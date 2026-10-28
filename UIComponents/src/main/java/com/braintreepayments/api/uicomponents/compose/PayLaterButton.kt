package com.braintreepayments.api.uicomponents.compose

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.coroutineScope
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayLaterButtonColor
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(true) }
    var flowLaunched by rememberSaveable { mutableStateOf(false) }

    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    if (registry == null) {
        paypalTokenizeCallback.onPayPalResult(
            PayPalResult.Failure(
                Exception(
                    "ActivityResultRegistry is null. ActivityResultRegistry cannot be null for this flow."
                )
            )
        )
        return
    }

    val payPalLauncher = remember { PayPalLauncher(registry) }
    val payPalClient = remember {
        PayPalClient(
            context = context,
            authorization = authorization,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    }

    PayLaterButtonView(style = style, enabled = enabled) {
        enabled = false
        payPalRequest.shouldOfferPayLater = true
        payPalClient.createPaymentAuthRequest(
            context = context,
            payPalRequest = payPalRequest
        ) { paymentAuthRequest: PayPalPaymentAuthRequest ->
            when (paymentAuthRequest) {
                is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                    activity?.let {
                        coroutineScope.launch {
                            completePayPalFlow(
                                payPalLauncher,
                                pendingRequestRepository,
                                it,
                                paymentAuthRequest,
                                paypalTokenizeCallback
                            )
                            flowLaunched = true
                        }
                    } ?: run {
                        enabled = true
                        paypalTokenizeCallback.onPayPalResult(PayPalResult.Failure(Exception("Activity is null")))
                    }
                }

                is PayPalPaymentAuthRequest.Failure -> {
                    paypalTokenizeCallback.onPayPalResult(PayPalResult.Failure(paymentAuthRequest.error))
                    enabled = true
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        if (flowLaunched) {
            flowLaunched = false
            lifecycle.coroutineScope.launch {
                val pendingRequestStr = pendingRequestRepository.getPendingRequest()

                activity?.intent?.let { intent ->
                    handleReturnToApp(payPalLauncher, payPalClient, pendingRequestStr, intent, paypalTokenizeCallback)
                    enabled = true
                    pendingRequestRepository.clearPendingRequest()
                } ?: run {
                    enabled = true
                    paypalTokenizeCallback.onPayPalResult(
                        PayPalResult.Failure(Exception("Activity or Intent is null. Unable to restore state."))
                    )
                }
            }
        }

        onPauseOrDispose { lifecycle }
    }
}
