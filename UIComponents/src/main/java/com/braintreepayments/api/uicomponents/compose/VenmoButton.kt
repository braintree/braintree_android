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
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics.UI_TYPE_COMPOSE
import com.braintreepayments.api.uicomponents.VenmoButtonColor
import com.braintreepayments.api.venmo.VenmoClient
import com.braintreepayments.api.venmo.VenmoLauncher
import com.braintreepayments.api.venmo.VenmoPaymentAuthRequest
import com.braintreepayments.api.venmo.VenmoPaymentAuthResult
import com.braintreepayments.api.venmo.VenmoPendingRequest
import com.braintreepayments.api.venmo.VenmoRequest
import com.braintreepayments.api.venmo.VenmoResult
import com.braintreepayments.api.venmo.VenmoTokenizeCallback
import kotlin.text.isEmpty
import kotlinx.coroutines.launch

/**
 * A composable that displays a Venmo branded button.
 * @param style: A [VenmoButtonColor] that determines the color of the button.
 * @param venmoRequest: A [VenmoRequest] that provides the parameters to tokenize a Venmo account.
 * @param authorization: An authorization string to use for tokenization.
 * @param appLinkReturnUrl: A [Uri] that sends back control to the host app after Venmo flow completes.
 * @param deepLinkFallbackUrlScheme: Fallback scheme in case [appLinkReturnUrl] doesn't work.
 * @param venmoTokenizeCallback: A [VenmoTokenizeCallback] that handles the result of the tokenization.
 */
@Composable
fun VenmoButton(
    style: VenmoButtonColor,
    venmoRequest: VenmoRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    deepLinkFallbackUrlScheme: String? = null,
    pendingRequestRepository: PendingRequestRepository = PendingRequestRepository(LocalContext.current, "venmo"),
    venmoTokenizeCallback: VenmoTokenizeCallback
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(true) }
    var flowLaunched by remember { mutableStateOf(false) }
    var shouldLogButtonPresentment by rememberSaveable { mutableStateOf(true) }

    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    if (registry == null) {
        venmoTokenizeCallback.onVenmoResult(
            VenmoResult.Failure(
                Exception(
                    "ActivityResultRegistry is null. ActivityResultRegistry cannot be null for this flow."
                )
            )
        )
        return
    }

    val venmoLauncher = remember { VenmoLauncher(registry) }
    val venmoClient = remember {
        VenmoClient(
            context = context,
            authorization = authorization,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    }
    val analyticsClient = remember { AnalyticsClient.lazyInstance.value }

    VenmoButtonView(style = style, enabled = enabled) {
        enabled = false
        logButtonSelected(analyticsClient)
        venmoClient.createPaymentAuthRequest(
            context = context,
            request = venmoRequest
        ) { paymentAuthRequest: VenmoPaymentAuthRequest ->
            when (paymentAuthRequest) {
                is VenmoPaymentAuthRequest.ReadyToLaunch -> {
                    activity?.let {
                        coroutineScope.launch {
                            completeVenmoFlow(
                                venmoLauncher,
                                pendingRequestRepository,
                                it,
                                paymentAuthRequest,
                                venmoTokenizeCallback
                            )
                            flowLaunched = true
                        }
                    }
                }

                is VenmoPaymentAuthRequest.Failure -> {
                    venmoTokenizeCallback.onVenmoResult(VenmoResult.Failure(paymentAuthRequest.error))
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
                val pendingRequestString = pendingRequestRepository.getPendingRequest()

                activity?.intent?.let { intent ->
                    handleReturnToApp(venmoLauncher, venmoClient, pendingRequestString, intent, venmoTokenizeCallback)
                    enabled = true
                    pendingRequestRepository.clearPendingRequest()
                    activity.intent.data = null
                }
            }
        }

        onPauseOrDispose { lifecycle }
    }
}

private suspend fun completeVenmoFlow(
    venmoLauncher: VenmoLauncher,
    pendingRequestRepository: PendingRequestRepository,
    activity: Activity,
    paymentAuthRequest: VenmoPaymentAuthRequest.ReadyToLaunch,
    venmoTokenizeCallback: VenmoTokenizeCallback
) {
    val venmoPendingRequest = venmoLauncher.launch(
        activity = activity as ComponentActivity,
        paymentAuthRequest = paymentAuthRequest
    )
    when (venmoPendingRequest) {
        is VenmoPendingRequest.Started -> {
            pendingRequestRepository.storePendingRequest(venmoPendingRequest.pendingRequestString)
        }

        is VenmoPendingRequest.Failure -> {
            venmoTokenizeCallback.onVenmoResult(VenmoResult.Failure(venmoPendingRequest.error))
        }
    }
}

private fun handleReturnToApp(
    venmoLauncher: VenmoLauncher,
    venmoClient: VenmoClient,
    pendingRequestString: String,
    intent: Intent,
    callback: VenmoTokenizeCallback
) {
    if (pendingRequestString.isEmpty()) {
        callback.onVenmoResult(VenmoResult.Failure(Exception(PendingRequestException())))
        return
    }
    val paymentAuthResult = venmoLauncher.handleReturnToApp(
        pendingRequest = VenmoPendingRequest.Started(pendingRequestString),
        intent = intent
    )
    when (paymentAuthResult) {
        is VenmoPaymentAuthResult.Success -> {
            venmoClient.tokenize(paymentAuthResult) { venmoResult ->
                callback.onVenmoResult(venmoResult)
            }
        }
        is VenmoPaymentAuthResult.Failure -> {
            callback.onVenmoResult(VenmoResult.Failure(paymentAuthResult.error))
        }
        is VenmoPaymentAuthResult.NoResult -> {
            callback.onVenmoResult(VenmoResult.Cancel)
        }
    }
}

private fun logButtonPresented(analyticsClient: AnalyticsClient) {
    analyticsClient.sendEvent(
        UIComponentsAnalytics.VENMO_BUTTON_PRESENTED,
        AnalyticsEventParams(uiType = UI_TYPE_COMPOSE)
    )
}

private fun logButtonSelected(analyticsClient: AnalyticsClient) {
    analyticsClient.sendEvent(
        UIComponentsAnalytics.VENMO_BUTTON_SELECTED,
        AnalyticsEventParams(uiType = UI_TYPE_COMPOSE)
    )
}
