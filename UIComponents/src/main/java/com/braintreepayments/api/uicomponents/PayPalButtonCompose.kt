package com.braintreepayments.api.uicomponents

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback

@Composable
fun PayPalButtonComposeImpl(
    payPalRequest: PayPalRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String,
    paypalLaunchCallback: PayPalLaunchCallback? = null,
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    // this might not be needed with LifecycleResumeEffect
//    val activityLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
//            handleReturnToApp()
//        }
    val context = LocalContext.current
    val activity = context.findActivity()
    val payPalLauncher = PayPalLauncher()
    val payPalClient = PayPalClient(
        context = context,
        authorization = authorization,
        appLinkReturnUrl = appLinkReturnUrl,
        deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
    )
    PayPalButtonCompose(color = PayPalButtonColor.Blue, enabled = true, loading = false) {
        // do something on click
        Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()

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
                            paypalLaunchCallback?.onPayPalPaymentAuthRequest(
                                PayPalPendingRequest.Started(payPalPendingRequest.pendingRequestString)
                            )
                            storePayPalPendingRequest(PayPalPendingRequest.Started(payPalPendingRequest.pendingRequestString))
                        }

                        is PayPalPendingRequest.Failure -> {
                            paypalLaunchCallback?.onPayPalPaymentAuthRequest(
                                PayPalPendingRequest.Failure(payPalPendingRequest.error)
                            )
                        }
                    }
//                    completePayPalFlow(paymentAuthRequest)
                }

                is PayPalPaymentAuthRequest.Failure -> {
                    paypalLaunchCallback?.onPayPalPaymentAuthRequest(
                        PayPalPendingRequest.Failure(paymentAuthRequest.error)
                    )
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        // Do something on resume or launch effect
        val pendingRequest = getPayPalPendingRequest()

        activity?.intent?.let { intent ->
            handleReturnToApp(payPalLauncher, payPalClient, pendingRequest, intent, paypalTokenizeCallback)
        }

        onPauseOrDispose {
            // Do something on pause or dispose effect
        }
    }
}

private var pendingRequest: String? = null

private fun storePayPalPendingRequest(request: PayPalPendingRequest.Started) {
    pendingRequest = request.pendingRequestString
}

private fun getPayPalPendingRequest(): PayPalPendingRequest.Started {
    return PayPalPendingRequest.Started(pendingRequest ?: "")
}

private fun clearPayPalPendingRequest() {
    pendingRequest = null
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun handleReturnToApp(
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
//    setButtonReEnabled()
}

@Composable
fun PayPalButtonCompose(color: PayPalButtonColor, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    val color: Color = when (color) {
        PayPalButtonColor.Blue -> {
            Color.Blue
        }
        PayPalButtonColor.Black -> {
            Color.Black
        }
        else -> {
            Color.White
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Button(
            onClick = { onClick() },
            modifier = Modifier
                .height(48.dp)
                .width(200.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)) {
            if(!loading && enabled) {
                Text(text = "Fancy PayPal Button")
            }
            if(loading) {
                Text(text = "Loading...")
            }
        }
    }
}
