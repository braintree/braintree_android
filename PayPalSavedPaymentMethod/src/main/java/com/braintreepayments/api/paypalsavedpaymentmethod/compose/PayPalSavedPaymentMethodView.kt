package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodViewModel
import com.braintreepayments.api.paypalsavedpaymentmethod.defaultClientFactory
import com.braintreepayments.api.paypalsavedpaymentmethod.findActivity
import com.braintreepayments.api.paypalsavedpaymentmethod.openUrl
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import kotlinx.coroutines.launch
import java.util.Objects
import java.util.UUID

/**
 * Shows the buyer's saved PayPal funding instrument and an optional Pay Later message, and lets the buyer change the
 * funding instrument in the PayPal paysheet. When the buyer returns, it tokenizes the PayPal account, delivers the
 * result to [paypalTokenizeCallback] and refreshes the funding instrument.
 *
 * Requires a client token generated with the buyer's payment method ID. The host activity must call
 * `setIntent(intent)` in `onNewIntent`. The PayPal return URL is removed from the intent once handled.
 *
 * State is kept in the hosting activity or navigation destination until it's destroyed, so show one per screen, not
 * one per list item. If the screen closes while the edit is being completed, its result isn't delivered.
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
@Suppress("LongParameterList")
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
    val context = LocalContext.current
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
            PayPalSavedPaymentMethodViewModel.Factory(
                factoryAuthorization,
                factoryAppLinkReturnUrl,
                factoryDeepLinkFallbackUrlScheme,
                defaultClientFactory(context)
            )
        },
        createPendingRequestRepository = { key -> PendingRequestRepository(context, key) }
    )
}

/** The component with the view model and pending request storage injectable, for tests. */
@OptIn(ExperimentalBetaApi::class)
@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun PayPalSavedPaymentMethodView(
    payPalCheckoutRequest: PayPalCheckoutRequest,
    request: PayPalSavedPaymentMethodRequest,
    authorization: String,
    appLinkReturnUrl: Uri,
    modifier: Modifier,
    deepLinkFallbackUrlScheme: String?,
    style: PayPalSavedPaymentMethodViewStyle,
    paypalTokenizeCallback: PayPalTokenizeCallback,
    viewModelFactory: (String, Uri, String?) -> ViewModelProvider.Factory,
    createPendingRequestRepository: (key: String) -> PendingRequestRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentCheckoutRequest by rememberUpdatedState(payPalCheckoutRequest)
    val currentCallback by rememberUpdatedState(paypalTokenizeCallback)

    // Saved, so the view model and pending request are found again after recreation or process death. A new
    // authorization or return URL gets its own, so an earlier edit isn't completed by it. The key is hashed because
    // it goes into saved state and the authorization mustn't.
    val instanceId = rememberSaveable { UUID.randomUUID().toString() }
    val viewModelKey = "PayPalSavedPaymentMethodView:$instanceId:" +
        Objects.hash(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)
    val pendingRequestRepository = remember(viewModelKey) { createPendingRequestRepository(viewModelKey) }
    // Pending request of the edit in progress. This copy covers a failed repository write.
    var launchedPendingRequest by rememberSaveable(viewModelKey) { mutableStateOf<String?>(null) }

    val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
    if (registry == null) {
        LaunchedEffect(Unit) {
            val error = BraintreeException(NO_ACTIVITY_RESULT_REGISTRY_MESSAGE)
            currentCallback.onPayPalResult(PayPalResult.Failure(error))
        }
        return
    }
    val payPalLauncher = remember(registry) { PayPalLauncher(registry) }

    // One per instance and authorization.
    val viewModel: PayPalSavedPaymentMethodViewModel = viewModel(
        key = viewModelKey,
        factory = remember(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme) {
            viewModelFactory(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)
        }
    )
    val state by viewModel.state.collectAsState()

    // The first load fetches both; later request changes reload the message.
    LaunchedEffect(viewModel) {
        viewModel.load(request, style.showPayPalCreditMessaging)
    }
    LaunchedEffect(viewModel, request, style.showPayPalCreditMessaging) {
        viewModel.onRequestChanged(request, style.showPayPalCreditMessaging)
    }

    // Clears the stored pending request once its result is delivered.
    LaunchedEffect(viewModel) {
        viewModel.results.collect { result ->
            launchedPendingRequest = null
            pendingRequestRepository.clear()
            currentCallback.onPayPalResult(result)
        }
    }

    LifecycleResumeEffect(payPalLauncher, viewModel, pendingRequestRepository) {
        coroutineScope.launch {
            val pendingRequest = launchedPendingRequest ?: pendingRequestRepository.get() ?: return@launch
            viewModel.handleReturnToApp(pendingRequest, payPalLauncher, context.findActivity()?.intent)
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
                    is PayPalPendingRequest.Started -> {
                        launchedPendingRequest = pending.pendingRequestString
                        // Best effort: the paysheet is already open.
                        pendingRequestRepository.store(pending.pendingRequestString)
                    }
                    is PayPalPendingRequest.Failure ->
                        currentCallback.onPayPalResult(PayPalResult.Failure(pending.error))
                    null -> Unit
                }
            }
        },
        onLearnMoreClick = { url -> context.openUrl(url) },
        modifier = modifier
    )

    if (state.isEditing) {
        LoadingDialog()
    }
}

private const val NO_ACTIVITY_RESULT_REGISTRY_MESSAGE =
    "PayPalSavedPaymentMethodView must be hosted in an activity that provides an ActivityResultRegistry."
