package com.braintreepayments.api.uicomponents.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonColor

private var pendingRequest: String? = null

@Composable
fun PayPalSmartButton(
    style: PayPalButtonColor,
    payPalRequest: PayPalRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String,
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    var enabled by remember { mutableStateOf(true) }

    val viewModel: PayPalComposeButtonViewModel =
        viewModel { PayPalComposeButtonViewModel(PayPalPendingRequestRepository()) }

    val payPalLauncher = PayPalLauncher(LocalActivityResultRegistryOwner.current?.activityResultRegistry)
    val payPalClient = PayPalClient(
        context = context,
        authorization = authorization,
        appLinkReturnUrl = appLinkReturnUrl,
        deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
    )
    PayPalButton(style = style, enabled = enabled) {
        enabled = false
        payPalClient.createPaymentAuthRequest(
            context = context,
            payPalRequest = payPalRequest
        ) { paymentAuthRequest: PayPalPaymentAuthRequest ->
            when (paymentAuthRequest) {
                is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                    activity?.let {
                        completePayPalFlow(payPalLauncher, it, paymentAuthRequest, viewModel)
                    }
                }

                is PayPalPaymentAuthRequest.Failure -> {
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        // Do something on resume or launch effect
        val pendingRequest = getPayPalPendingRequest()

        activity?.intent?.let { intent ->
            handleReturnToApp(payPalLauncher, payPalClient, pendingRequest, intent, paypalTokenizeCallback)
            enabled = true
            clearPayPalPendingRequest()
            activity.intent.data = null
        }

        onPauseOrDispose {
            // Do something on pause or dispose effect
//            viewModel.clearPayPalPendingRequest()
        }
    }
}

internal fun completePayPalFlow(
    payPalLauncher: PayPalLauncher,
    activity: Activity,
    paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch,
    viewModel: PayPalComposeButtonViewModel
) {
    val payPalPendingRequest = payPalLauncher.launch(
        activity = activity as ComponentActivity,
        paymentAuthRequest = paymentAuthRequest
    )
    when (payPalPendingRequest) {
        is PayPalPendingRequest.Started -> {
            storePayPalPendingRequest(PayPalPendingRequest.Started(payPalPendingRequest.pendingRequestString))
        }

        is PayPalPendingRequest.Failure -> {
        }
    }
}

private fun handleReturnToApp(
    payPalLauncher: PayPalLauncher,
    payPalClient: PayPalClient,
    pendingRequest: PayPalPendingRequest.Started,
    intent: Intent,
    callback: PayPalTokenizeCallback
) {
    val paymentAuthResult = payPalLauncher.handleReturnToApp(
        pendingRequest = pendingRequest,
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

private fun getPayPalPendingRequest(): PayPalPendingRequest.Started {
    return PayPalPendingRequest.Started(pendingRequest ?: "")
}

private fun storePayPalPendingRequest(request: PayPalPendingRequest.Started) {
    pendingRequest = request.pendingRequestString
}

private fun clearPayPalPendingRequest() {
    pendingRequest = null
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
