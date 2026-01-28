package com.braintreepayments.api.uicomponents

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalTokenizeCallback

@Composable
fun PayPalButtonComposeImpl(
    payPalRequest: PayPalRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String,
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    var loading by remember { mutableStateOf(false) }

    val viewModel: PayPalComposeButtonViewModel =
        viewModel { PayPalComposeButtonViewModel(PayPalPendingRequestRepository()) }

    val payPalLauncher = PayPalLauncher(LocalActivityResultRegistryOwner.current?.activityResultRegistry)
    val payPalClient = PayPalClient(
        context = context,
        authorization = authorization,
        appLinkReturnUrl = appLinkReturnUrl,
        deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
    )
    PayPalButtonCompose(color = PayPalButtonColor.Blue, loading = loading) {
        loading = true
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
        val pendingRequest = viewModel.getPayPalPendingRequest()

        activity?.intent?.let { intent ->
            viewModel.handleReturnToApp(payPalLauncher, payPalClient, pendingRequest, intent, paypalTokenizeCallback)
            loading = false
            viewModel.clearPayPalPendingRequest()
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
            viewModel.storePayPalPendingRequest(PayPalPendingRequest.Started(payPalPendingRequest.pendingRequestString))
        }

        is PayPalPendingRequest.Failure -> {
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
