package com.braintreepayments.api.uicomponents

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
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
    // this might still be needed for auth tab, but the impl might have to change to take in a launcher instead of a
    // caller
//    val activityLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
//            handleReturnToApp()
//        }
    val context = LocalContext.current
    val activity = context.findActivity()

    val viewModel: PayPalComposeButtonViewModel = viewModel { PayPalComposeButtonViewModel() }

    val payPalLauncher = PayPalLauncher()
    val payPalClient = PayPalClient(
        context = context,
        authorization = authorization,
        appLinkReturnUrl = appLinkReturnUrl,
        deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
    )
    PayPalButtonCompose(color = PayPalButtonColor.Blue, enabled = true, loading = false) {
        payPalClient.createPaymentAuthRequest(
            context = context,
            payPalRequest = payPalRequest
        ) { paymentAuthRequest: PayPalPaymentAuthRequest ->
            when (paymentAuthRequest) {
                is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                    val payPalPendingRequest = payPalLauncher.launch(
                        activity = activity as ComponentActivity,
                        paymentAuthRequest = paymentAuthRequest
                    )
                    when (payPalPendingRequest) {
                        is PayPalPendingRequest.Started -> {
//                            paypalLaunchCallback?.onPayPalPaymentAuthRequest(
//                                PayPalPendingRequest.Started(payPalPendingRequest.pendingRequestString)
//                            )
                            viewModel.storePayPalPendingRequest(PayPalPendingRequest.Started(payPalPendingRequest.pendingRequestString))
                        }

                        is PayPalPendingRequest.Failure -> {
//                            paypalLaunchCallback?.onPayPalPaymentAuthRequest(
//                                PayPalPendingRequest.Failure(payPalPendingRequest.error)
//                            )
                        }
                    }
//                    completePayPalFlow(paymentAuthRequest)
                }

                is PayPalPaymentAuthRequest.Failure -> {
//                    paypalLaunchCallback?.onPayPalPaymentAuthRequest(
//                        PayPalPendingRequest.Failure(paymentAuthRequest.error)
//                    )
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        // Do something on resume or launch effect
        val pendingRequest = viewModel.getPayPalPendingRequest()

        activity?.intent?.let { intent ->
            viewModel.handleReturnToApp(payPalLauncher, payPalClient, pendingRequest, intent, paypalTokenizeCallback)
        }

        onPauseOrDispose {
            // Do something on pause or dispose effect
            viewModel.clearPayPalPendingRequest()
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
