package com.braintreepayments.api.uicomponents.compose

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.coroutineScope
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import kotlinx.coroutines.launch

/**
 * Hosts the PayPal browser-switch flow shared by the PayPal and PayPal Pay Later buttons: it creates the client and
 * launcher, launches the flow when the button is clicked and completes it when the app is resumed.
 *
 * The button itself is supplied by the caller through [button], which receives whether it is enabled and the click
 * handler it must invoke.
 *
 * @param onButtonSelected Invoked when the button is clicked, before the payment auth request is created.
 * @param button The button to render.
 */
@Composable
internal fun PayPalButtonFlow(
    payPalRequest: PayPalRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String,
    pendingRequestRepository: PendingRequestRepository,
    paypalTokenizeCallback: PayPalTokenizeCallback,
    onButtonSelected: () -> Unit = {},
    button: @Composable (enabled: Boolean, onClick: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(true) }
    var flowLaunched by rememberSaveable { mutableStateOf(false) }

    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    if (registry == null) {
        LaunchedEffect(Unit) {
            paypalTokenizeCallback.onPayPalResult(
                PayPalResult.Failure(
                    Exception(
                        "ActivityResultRegistry is null. ActivityResultRegistry cannot be null for this flow."
                    )
                )
            )
        }
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

    button(enabled) {
        enabled = false
        onButtonSelected()
        payPalClient.createPaymentAuthRequest(
            context = context,
            payPalRequest = payPalRequest
        ) { paymentAuthRequest: PayPalPaymentAuthRequest ->
            when (paymentAuthRequest) {
                is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                    activity?.let {
                        coroutineScope.launch {
                            val launched = completePayPalFlow(
                                payPalLauncher,
                                pendingRequestRepository,
                                it,
                                paymentAuthRequest,
                                paypalTokenizeCallback
                            )
                            if (launched) {
                                flowLaunched = true
                            } else {
                                enabled = true
                            }
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

/**
 * Launches the PayPal flow and stores the pending request.
 *
 * @return `true` if the flow was started, `false` if it failed to launch (the failure is reported to the callback).
 */
internal suspend fun completePayPalFlow(
    payPalLauncher: PayPalLauncher,
    pendingRequestRepository: PendingRequestRepository,
    activity: Activity,
    paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch,
    paypalTokenizeCallback: PayPalTokenizeCallback
): Boolean {
    val componentActivity = activity as? ComponentActivity
        ?: run {
            paypalTokenizeCallback.onPayPalResult(
                PayPalResult.Failure(Exception("Parent activity is expected to be a ComponentActivity"))
            )
            return false
        }
    val payPalPendingRequest = payPalLauncher.launch(
        activity = componentActivity,
        paymentAuthRequest = paymentAuthRequest
    )
    return when (payPalPendingRequest) {
        is PayPalPendingRequest.Started -> {
            pendingRequestRepository.storePendingRequest(payPalPendingRequest.pendingRequestString)
            true
        }

        is PayPalPendingRequest.Failure -> {
            paypalTokenizeCallback.onPayPalResult(PayPalResult.Failure(payPalPendingRequest.error))
            false
        }
    }
}

internal fun handleReturnToApp(
    payPalLauncher: PayPalLauncher,
    payPalClient: PayPalClient,
    pendingRequestString: String,
    intent: Intent,
    callback: PayPalTokenizeCallback
) {
    if (pendingRequestString.isEmpty()) {
        callback.onPayPalResult(PayPalResult.Failure(PendingRequestException()))
        return
    }
    val paymentAuthResult = payPalLauncher.handleReturnToApp(
        pendingRequest = PayPalPendingRequest.Started(pendingRequestString),
        intent = intent,
    )

    when (paymentAuthResult) {
        is PayPalPaymentAuthResult.Success -> {
            payPalClient.tokenize(paymentAuthResult) { payPalResult ->
                callback.onPayPalResult(payPalResult)
            }
        }

        is PayPalPaymentAuthResult.NoResult -> {
            callback.onPayPalResult(PayPalResult.Cancel)
        }

        is PayPalPaymentAuthResult.Failure -> {
            callback.onPayPalResult(PayPalResult.Failure(paymentAuthResult.error))
        }
    }
}