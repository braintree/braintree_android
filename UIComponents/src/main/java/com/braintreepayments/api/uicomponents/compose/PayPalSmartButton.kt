package com.braintreepayments.api.uicomponents.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics.UI_TYPE_COMPOSE
import kotlinx.coroutines.launch

/**
 * A composable that displays PayPal button.
 * @param style: A [PayPalButtonColor] that determines the color of the button.
 * @param payPalRequest: A [PayPalRequest] that provides the parameters to tokenize a paypal account.
 * @param authorization: An authorization string to use for tokenization.
 * @param appLinkReturnUrl: A [Uri] that sends back control to the host app after PayPal flow completes.
 * @param deepLinkFallbackUrlScheme: Fallback scheme in case [appLinkReturnUrl] doesn't work.
 * @param paypalTokenizeCallback: A [PayPalTokenizeCallback] that handles the result of the tokenization.
 */
@Composable
fun PayPalSmartButton(
    style: PayPalButtonColor,
    payPalRequest: PayPalRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String,
    pendingRequestRepository: PayPalPendingRequestRepository = PayPalPendingRequestRepository(LocalContext.current),
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(true) }
    var flowLaunched by remember { mutableStateOf(false) }
    var shouldLogButtonPresentment by rememberSaveable { mutableStateOf(true) }

    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    val payPalLauncher = remember { PayPalLauncher(registry) }
    val payPalClient = remember {
        PayPalClient(
            context = context,
            authorization = authorization,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    }
    val analyticsClient = remember { AnalyticsClient.lazyInstance.value }

    PayPalButton(style = style, enabled = enabled) {
        enabled = false
        logButtonSelected(analyticsClient)
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
                    }
                }

                is PayPalPaymentAuthRequest.Failure -> {
                    paypalTokenizeCallback.onPayPalResult(PayPalResult.Failure(paymentAuthRequest.error))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (shouldLogButtonPresentment) {
            shouldLogButtonPresentment = false
            logButtonPresented(analyticsClient)
        }
    }

    LifecycleResumeEffect(Unit) {
        if (flowLaunched) {
            flowLaunched = false
            lifecycle.coroutineScope.launch {
                val pendingRequest = pendingRequestRepository.getPendingRequest()

                activity?.intent?.let { intent ->
                    handleReturnToApp(payPalLauncher, payPalClient, pendingRequest, intent, paypalTokenizeCallback)
                    enabled = true
                    pendingRequestRepository.clearPendingRequest()
                    activity.intent.data = null
                }
            }
        }

        onPauseOrDispose { lifecycle }
    }
}

internal suspend fun completePayPalFlow(
    payPalLauncher: PayPalLauncher,
    pendingRequestRepository: PayPalPendingRequestRepository,
    activity: Activity,
    paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch,
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    val payPalPendingRequest = payPalLauncher.launch(
        activity = activity as ComponentActivity,
        paymentAuthRequest = paymentAuthRequest
    )
    when (payPalPendingRequest) {
        is PayPalPendingRequest.Started -> {
            pendingRequestRepository.storePendingRequest(payPalPendingRequest.pendingRequestString)
        }

        is PayPalPendingRequest.Failure -> {
            paypalTokenizeCallback.onPayPalResult(PayPalResult.Failure(payPalPendingRequest.error))
        }
    }
}

private fun handleReturnToApp(
    payPalLauncher: PayPalLauncher,
    payPalClient: PayPalClient,
    pendingRequest: String?,
    intent: Intent,
    callback: PayPalTokenizeCallback
) {
    val paymentAuthResult = payPalLauncher.handleReturnToApp(
        pendingRequest = PayPalPendingRequest.Started(pendingRequest ?: ""),
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

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
