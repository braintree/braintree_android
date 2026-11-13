package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodReturnResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodViewModel
import com.braintreepayments.api.paypalsavedpaymentmethod.clearPayPalReturnUrl
import com.braintreepayments.api.paypalsavedpaymentmethod.findActivity
import com.braintreepayments.api.paypalsavedpaymentmethod.openLearnMore
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Shows the buyer's saved PayPal funding instrument and an optional Pay Later message, and lets the buyer change the
 * funding instrument in the PayPal paysheet. When the buyer returns, it tokenizes the PayPal account, delivers the
 * result to [paypalTokenizeCallback] and refreshes the funding instrument.
 *
 * Requires a client token generated with the buyer's payment method ID. The host activity must call
 * `setIntent(intent)` in `onNewIntent`. The PayPal return URL is removed from the intent once handled.
 *
 * The [authorization] and return URLs are read when the component first appears, as on iOS, so a refreshed client
 * token doesn't lose the buyer's edit or a return from PayPal. To start over, for example for another buyer, key the
 * component on the authorization: `key(authorization) { PayPalSavedPaymentMethodView(...) }`.
 *
 * State is kept in the hosting activity or navigation destination until it's destroyed, so show one per screen, not
 * one per list item, and keep it in the composition for as long as the screen is shown. Removing it and adding it back
 * starts a new instance that doesn't know about an edit completed by the earlier one. If the screen closes while the
 * edit is being completed, its result isn't delivered.
 *
 * @param payPalCheckoutRequest the checkout the edited funding instrument applies to.
 * @param request the amount and currency for the Pay Later message, and an optional merchant account. A change reloads
 * the message.
 * @param authorization a client token carrying the buyer's payment method ID.
 * @param appLinkReturnUrl the Android App Link used to return to your app from PayPal.
 * @param modifier the [Modifier] applied to the component.
 * @param deepLinkFallbackUrlScheme a return URL scheme used when the App Link isn't available.
 * @param style the component's styling.
 * @param paypalTokenizeCallback receives the result of the edit: success with the nonce, cancel, or failure.
 */
@ExperimentalBetaApi
@Composable
fun PayPalSavedPaymentMethodView(
    payPalCheckoutRequest: PayPalCheckoutRequest,
    request: PayPalSavedPaymentMethodRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    modifier: Modifier = Modifier,
    deepLinkFallbackUrlScheme: String? = null,
    style: PayPalSavedPaymentMethodViewStyle = PayPalSavedPaymentMethodViewStyle(),
    paypalTokenizeCallback: PayPalTokenizeCallback
) {
    // The application context, so the client doesn't hold the activity.
    val applicationContext = LocalContext.current.applicationContext
    PayPalSavedPaymentMethodView(
        payPalCheckoutRequest = payPalCheckoutRequest,
        request = request,
        authorization = authorization,
        appLinkReturnUrl = appLinkReturnUrl,
        modifier = modifier,
        deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme,
        style = style,
        paypalTokenizeCallback = paypalTokenizeCallback,
        viewModelFactory = { factoryAuthorization, factoryAppLinkReturnUrl, factoryDeepLinkFallbackUrlScheme ->
            PayPalSavedPaymentMethodViewModel.Factory {
                PayPalSavedPaymentMethodClient(
                    applicationContext,
                    factoryAuthorization,
                    factoryAppLinkReturnUrl,
                    factoryDeepLinkFallbackUrlScheme
                )
            }
        }
    )
}

/** The component with the view model injectable, for tests. */
@OptIn(ExperimentalBetaApi::class)
@Composable
internal fun PayPalSavedPaymentMethodView(
    payPalCheckoutRequest: PayPalCheckoutRequest,
    request: PayPalSavedPaymentMethodRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    modifier: Modifier,
    deepLinkFallbackUrlScheme: String?,
    style: PayPalSavedPaymentMethodViewStyle,
    paypalTokenizeCallback: PayPalTokenizeCallback,
    viewModelFactory: (String, Uri, String?) -> ViewModelProvider.Factory
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentCheckoutRequest by rememberUpdatedState(payPalCheckoutRequest)
    val currentCallback by rememberUpdatedState(paypalTokenizeCallback)

    // Saved, so the view model is found again after recreation or process death.
    val instanceId = rememberSaveable { UUID.randomUUID().toString() }
    val viewModelKey = "PayPalSavedPaymentMethodView:$instanceId"
    // Read once, as on iOS, so a refreshed authorization keeps the view model and the edit in progress.
    val returnUrls = remember { ReturnUrls(appLinkReturnUrl, deepLinkFallbackUrlScheme) }
    val factory = remember { viewModelFactory(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme) }
    // Pending request of the edit in progress. Saved, so a return after process death can still be completed.
    var launchedPendingRequest by rememberSaveable { mutableStateOf<String?>(null) }

    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    if (registry == null) {
        LaunchedEffect(Unit) {
            val error = BraintreeException(NO_ACTIVITY_RESULT_REGISTRY_MESSAGE)
            currentCallback.onPayPalResult(PayPalResult.Failure(error))
        }
        return
    }
    val payPalLauncher = remember(registry) { PayPalLauncher(registry) }

    val viewModel: PayPalSavedPaymentMethodViewModel = viewModel(key = viewModelKey, factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The first load fetches both; later request changes reload the message.
    LaunchedEffect(viewModel) {
        viewModel.load(request, style.showPayPalCreditMessaging)
    }
    LaunchedEffect(viewModel, request, style.showPayPalCreditMessaging) {
        viewModel.onRequestChanged(request, style.showPayPalCreditMessaging)
    }

    // Forgets the pending request once its result is delivered. A cancel does too: every resume passes the pending
    // request to the view model, so keeping it would deliver the cancel again each time the screen resumes.
    LaunchedEffect(viewModel) {
        viewModel.results.collect { result ->
            launchedPendingRequest = null
            currentCallback.onPayPalResult(result)
        }
    }

    LifecycleResumeEffect(payPalLauncher, viewModel) {
        val pendingRequest = launchedPendingRequest
        val intent = context.findActivity()?.intent
        if (pendingRequest != null &&
            viewModel.takeReturn(pendingRequest, payPalLauncher, intent, returnUrls)
        ) {
            launchedPendingRequest = null
        }
        onPauseOrDispose { }
    }

    PayPalSavedPaymentMethodContent(
        fundingInstrumentState = state.fundingInstrument,
        creditMessagingState = state.creditMessaging,
        style = style,
        editEnabled = !state.isEditing,
        isEditCompleted = state.isEditCompleted,
        onEditClick = {
            coroutineScope.launch {
                when (val pending = viewModel.startEdit(context, payPalLauncher, currentCheckoutRequest)) {
                    is PayPalPendingRequest.Started -> launchedPendingRequest = pending.pendingRequestString
                    is PayPalPendingRequest.Failure ->
                        currentCallback.onPayPalResult(PayPalResult.Failure(pending.error))
                    null -> Unit
                }
            }
        },
        onLearnMoreClick = { learnMore -> context.openLearnMore(learnMore) },
        modifier = modifier
    )

    if (state.isEditing) {
        LoadingDialog()
    }
}

/**
 * Passes the PayPal return in [intent] to the view model and clears its return URL once the return is completed.
 * Returns whether the view model took it, so the pending request can be forgotten; one it didn't take, because an edit
 * step is running, is passed again on the next resume.
 */
@OptIn(ExperimentalBetaApi::class)
private fun PayPalSavedPaymentMethodViewModel.takeReturn(
    pendingRequest: String,
    payPalLauncher: PayPalLauncher,
    intent: Intent?,
    returnUrls: ReturnUrls
): Boolean = when (handleReturnToApp(pendingRequest, payPalLauncher, intent)) {
    PayPalSavedPaymentMethodReturnResult.NOT_TAKEN -> false
    PayPalSavedPaymentMethodReturnResult.TAKEN -> true
    PayPalSavedPaymentMethodReturnResult.COMPLETED -> {
        intent?.clearPayPalReturnUrl(returnUrls.appLink, returnUrls.deepLinkScheme)
        true
    }
}

// The return URLs the component was first shown with.
private class ReturnUrls(val appLink: Uri, val deepLinkScheme: String?)

private const val NO_ACTIVITY_RESULT_REGISTRY_MESSAGE =
    "PayPalSavedPaymentMethodView must be hosted in an activity that provides an ActivityResultRegistry."
