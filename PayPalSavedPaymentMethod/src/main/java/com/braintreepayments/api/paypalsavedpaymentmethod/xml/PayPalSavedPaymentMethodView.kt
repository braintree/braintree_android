package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodAnalytics
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodReturnResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodViewModel
import com.braintreepayments.api.paypalsavedpaymentmethod.clearPayPalReturnUrl
import com.braintreepayments.api.paypalsavedpaymentmethod.findActivity
import com.braintreepayments.api.paypalsavedpaymentmethod.openLearnMore
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.styleFromAttrs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Shows the buyer's saved PayPal funding instrument and an optional Pay Later message, and lets the buyer change the
 * funding instrument in the PayPal paysheet.
 *
 * Call [initialize] once the authorization is available. When the buyer taps the funding instrument, the view opens
 * the PayPal paysheet and passes the pending request to the [PayPalSavedPaymentMethodLaunchCallback]. Store it, and
 * when the buyer returns (in `onResume`), pass it to [handleReturnToApp] with the activity's intent. The result goes
 * to the [PayPalTokenizeCallback], and the funding instrument refreshes. Clear the stored request once
 * [handleReturnToApp] returns `true`.
 *
 * Requires a client token generated with the buyer's payment method ID, and a `ComponentActivity` host that calls
 * `setIntent(intent)` in `onNewIntent`. Call all methods and setters on the main thread.
 *
 * The view needs an `android:id` that is stable across recreation and unique in its activity or fragment, since views
 * with the same id share one view model. So it can't be used in list items. In a fragment, call [initialize] from
 * `onViewCreated` or later. Callbacks run only while the host is started, and a result isn't delivered if the screen
 * closes during an edit.
 *
 * Style it with the `paypalSavedPaymentMethod*` XML attributes or [style]; a style set in code replaces the attributes.
 *
 * Note: **This feature is in beta. Its public API may change in future releases.**
 */
@ExperimentalBetaApi
@Suppress("TooManyFunctions")
class PayPalSavedPaymentMethodView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val contentView = PayPalSavedPaymentMethodContentView(context)

    /** The checkout the edited funding instrument applies to. Read when the buyer taps it. */
    var payPalCheckoutRequest: PayPalCheckoutRequest? = null

    /** The amount and currency for the Pay Later message. A change reloads the message. */
    var request: PayPalSavedPaymentMethodRequest? = null
        set(value) {
            field = value
            if (value != null) viewModel?.onRequestChanged(value, style.showPayPalCreditMessaging)
        }

    /** The component's styling. Turning the Pay Later message back on loads it. */
    var style: PayPalSavedPaymentMethodViewStyle = styleFromAttrs(context, attrs, defStyleAttr)
        set(value) {
            field = value
            resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, value)
            render()
            request?.let { viewModel?.onRequestChanged(it, value.showPayPalCreditMessaging) }
        }

    private var resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, style)

    /** Creates the view model, with a client that holds the application context. Replaced in tests. */
    internal var viewModelFactory: (String, Uri, String?) -> ViewModelProvider.Factory =
        { authorization, returnUrl, deepLinkScheme ->
            val applicationContext = context.applicationContext
            PayPalSavedPaymentMethodViewModel.Factory { buttonSessionId ->
                PayPalSavedPaymentMethodClient(
                    applicationContext,
                    authorization,
                    returnUrl,
                    deepLinkScheme,
                    PayPalSavedPaymentMethodAnalytics.UI_TYPE_XML,
                    buttonSessionId
                )
            }
        }

    private var viewModel: PayPalSavedPaymentMethodViewModel? = null
    /**
     * Registered without a lifecycle, since [initialize] can run after the host has started, so it's never
     * unregistered. That's bounded: one registration per view id, replaced on recreation, released with the activity.
     */
    private var payPalLauncher: PayPalLauncher? = null
    private var launchCallback: PayPalSavedPaymentMethodLaunchCallback? = null
    private var tokenizeCallback: PayPalTokenizeCallback? = null
    /** What [initialize] was first called with. */
    private var setup: Setup? = null
    /** A return handled before the view model was found. */
    private var pendingReturn: Pair<PayPalPendingRequest.Started, Intent>? = null
    private var state = PayPalSavedPaymentMethodUiState()
    private var loadingDialog: EditLoadingDialog? = null
    /** Opening the paysheet. Back cancels it until then, not later, since that would lose an approved change. */
    private var editJob: Job? = null
    /** A setup failure not delivered yet. */
    private var pendingFailure: Runnable? = null

    /** Renders and delivers results while attached. */
    private var attachedScope: CoroutineScope? = null

    init {
        addView(contentView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        contentView.onEditClick = { startEdit() }
        contentView.onLearnMoreClick = { learnMore ->
            viewModel?.onLearnMoreSelected()
            context.openLearnMore(learnMore)
        }
        render()
    }

    /**
     * Sets up the view and loads the funding instrument and the Pay Later message. Calling it again updates the
     * checkout, the request and the callbacks. The authorization and return URLs from the first call are kept, so a
     * refreshed client token doesn't lose the buyer's edit or a return from PayPal. To use a new client token, use a
     * new view with a different `android:id`.
     *
     * @param authorization a client token carrying the buyer's payment method ID.
     * @param appLinkReturnUrl the Android App Link used to return to your app from PayPal.
     * @param payPalCheckoutRequest the checkout the edited funding instrument applies to.
     * @param request the amount and currency for the Pay Later message, and an optional merchant account.
     * @param launchCallback receives the pending request to store when the PayPal paysheet opens.
     * @param tokenizeCallback receives the result of the edit: success with the nonce, cancel, or failure.
     * @param deepLinkFallbackUrlScheme a return URL scheme used when the App Link is not available.
     */
    @JvmOverloads
    fun initialize(
        authorization: String,
        appLinkReturnUrl: Uri,
        payPalCheckoutRequest: PayPalCheckoutRequest,
        request: PayPalSavedPaymentMethodRequest,
        launchCallback: PayPalSavedPaymentMethodLaunchCallback,
        tokenizeCallback: PayPalTokenizeCallback,
        deepLinkFallbackUrlScheme: String? = null
    ) {
        this.payPalCheckoutRequest = payPalCheckoutRequest
        this.request = request
        this.launchCallback = launchCallback
        this.tokenizeCallback = tokenizeCallback
        val activity = context.findActivity() as? ComponentActivity
        when {
            activity == null -> fail(NO_COMPONENT_ACTIVITY_MESSAGE)
            id == NO_ID -> fail(NO_VIEW_ID_MESSAGE)
            else -> {
                if (setup == null) setup = Setup(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)
                val owner = viewModelStoreOwner()
                if (owner != null) bindViewModel(owner) else unbindViewModel()
            }
        }
    }

    /**
     * Completes the edit when the buyer returns from PayPal. Call it from `onResume` with the pending request stored
     * from the [PayPalSavedPaymentMethodLaunchCallback] and the activity's intent. A pending request that was already
     * handled is ignored. A return with no result, for example when the activity resumes while the paysheet is still
     * open, is delivered to the [PayPalTokenizeCallback] as a cancel. The PayPal return URL is removed from [intent]
     * once handled.
     *
     * Does nothing before [initialize].
     *
     * @return `true` if the view took the return or had already completed it, so the stored [pendingRequest] can be
     * cleared. `false` if it was not taken (before [initialize], or while another edit step is running) or the view
     * isn't attached yet; keep it and pass it again on the next `onResume`.
     */
    fun handleReturnToApp(pendingRequest: PayPalPendingRequest.Started, intent: Intent): Boolean {
        val setup = setup ?: return false
        val viewModel = viewModel
        if (viewModel == null) {
            pendingReturn = pendingRequest to intent
            return false
        }
        val payPalLauncher = payPalLauncher ?: return false
        return when (viewModel.handleReturnToApp(pendingRequest.pendingRequestString, payPalLauncher, intent)) {
            PayPalSavedPaymentMethodReturnResult.NOT_TAKEN -> false
            PayPalSavedPaymentMethodReturnResult.TAKEN -> true
            PayPalSavedPaymentMethodReturnResult.COMPLETED -> {
                intent.clearPayPalReturnUrl(setup.appLinkReturnUrl, setup.deepLinkFallbackUrlScheme)
                true
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (setup != null && viewModel == null) {
            viewModelStoreOwner()?.let { bindViewModel(it) }
            return
        }
        val viewModel = viewModel ?: return
        observeViewModel(viewModel)
    }

    override fun onDetachedFromWindow() {
        pendingFailure?.let { removeCallbacks(it) }
        pendingFailure = null
        attachedScope?.cancel()
        attachedScope = null
        loadingDialog?.dismiss()
        loadingDialog = null
        super.onDetachedFromWindow()
    }

    /** A host that handles font scale or density changes itself keeps this view, so sizes are resolved again. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, style)
        render()
    }

    /** Finds this view's view model, completes a return handled before it was found, and starts observing. */
    private fun bindViewModel(owner: ViewModelStoreOwner) {
        val setup = setup ?: return
        val factory = viewModelFactory(setup.authorization, setup.appLinkReturnUrl, setup.deepLinkFallbackUrlScheme)
        val key = "$VIEW_MODEL_KEY_PREFIX$id"
        val viewModel = ViewModelProvider(owner, factory)[key, PayPalSavedPaymentMethodViewModel::class.java]
        if (viewModel !== this.viewModel) unbindViewModel()
        this.viewModel = viewModel
        // Keyed by the view model, not the view id, so same-id views in different fragments don't share a result.
        // Kept on a later bind, since the paysheet's result goes to the launcher that opened it.
        if (payPalLauncher == null) {
            (context.findActivity() as? ComponentActivity)?.let {
                payPalLauncher = PayPalLauncher(it.activityResultRegistry, VIEW_MODEL_KEY_PREFIX + viewModel.resultKey)
            }
        }
        request?.let { viewModel.load(it, style.showPayPalCreditMessaging) }
        pendingReturn?.let { (pendingRequest, intent) -> handleReturnToApp(pendingRequest, intent) }
        pendingReturn = null
        if (isAttachedToWindow && attachedScope == null) observeViewModel(viewModel)
    }

    /** Stops showing the current view model. */
    private fun unbindViewModel() {
        viewModel = null
        attachedScope?.cancel()
        attachedScope = null
    }

    /** Hides the view and posts the reason, so the callback never runs inside [initialize]. */
    private fun fail(message: String) {
        unbindViewModel()
        setup = null
        pendingReturn = null
        state = state.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Hidden, isEditing = false)
        render()
        val callback = tokenizeCallback
        pendingFailure?.let { removeCallbacks(it) }
        val delivery = Runnable {
            pendingFailure = null
            callback?.onPayPalResult(PayPalResult.Failure(BraintreeException(message)))
        }
        pendingFailure = delivery
        post(delivery)
    }

    /** Opens the PayPal paysheet when the buyer taps the funding instrument. */
    private fun startEdit() {
        val viewModel = viewModel ?: return
        val payPalLauncher = payPalLauncher ?: return
        val payPalCheckoutRequest = payPalCheckoutRequest ?: return
        val scope = attachedScope
        val hostLifecycle = hostLifecycleOwner()?.lifecycle
        if (scope == null || hostLifecycle == null) return
        editJob = scope.launch {
            val pendingRequest = viewModel.startEdit(context, payPalLauncher, payPalCheckoutRequest, hostLifecycle)
            when (pendingRequest) {
                is PayPalPendingRequest.Started -> launchCallback?.onPayPalPendingRequest(pendingRequest)
                is PayPalPendingRequest.Failure ->
                    tokenizeCallback?.onPayPalResult(PayPalResult.Failure(pendingRequest.error))
                null -> Unit
            }
        }
    }

    /**
     * Renders state and delivers results while the host is started. Cancelled when the host ends or the view detaches,
     * since the view can be removed from a host that's still showing.
     */
    private fun observeViewModel(viewModel: PayPalSavedPaymentMethodViewModel) {
        attachedScope?.cancel()
        attachedScope = null
        val owner = hostLifecycleOwner() ?: return
        val hostJob = owner.lifecycleScope.coroutineContext.job
        attachedScope = CoroutineScope(SupervisorJob(hostJob) + Dispatchers.Main).also { scope ->
            scope.launch {
                owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.state.collect {
                            state = it
                            render()
                            viewModel.onRendered(style)
                        }
                    }
                    launch { viewModel.results.collect { tokenizeCallback?.onPayPalResult(it) } }
                }
            }
        }
    }

    private fun hostLifecycleOwner(): LifecycleOwner? =
        findViewTreeLifecycleOwner() ?: context.findActivity() as? LifecycleOwner

    /** Renders the state. The loading dialog shows only while attached, so it can't be left open. */
    private fun render() {
        contentView.render(
            fundingInstrumentState = state.fundingInstrument,
            creditMessagingState = state.creditMessaging,
            resolvedStyle = resolvedStyle,
            editEnabled = !state.isEditing,
            isEditCompleted = state.isEditCompleted
        )
        if (state.isEditing && isAttachedToWindow) {
            if (loadingDialog == null) loadingDialog = EditLoadingDialog(context) { cancelEdit() }.also { it.show() }
            loadingDialog?.setCancelable(editJob?.isActive == true)
        } else {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }

    /** The buyer backed out before the paysheet opened. */
    private fun cancelEdit() {
        editJob?.cancel()
        editJob = null
        tokenizeCallback?.onPayPalResult(PayPalResult.Cancel)
    }

    private companion object {
        const val VIEW_MODEL_KEY_PREFIX = "PayPalSavedPaymentMethodView:"
        const val NO_COMPONENT_ACTIVITY_MESSAGE = "PayPalSavedPaymentMethodView must be hosted in a ComponentActivity."
        const val NO_VIEW_ID_MESSAGE = "PayPalSavedPaymentMethodView needs an android:id."
    }
}

/** The authorization and return URLs, read once. */
private class Setup(val authorization: String, val appLinkReturnUrl: Uri, val deepLinkFallbackUrlScheme: String?)

/** The hosting fragment or activity; a view in a window with no owner, such as a dialog, uses the activity's. */
private fun View.viewModelStoreOwner(): ViewModelStoreOwner? = findViewTreeViewModelStoreOwner()
    ?: (context.findActivity() as? ViewModelStoreOwner)?.takeIf { isAttachedToWindow }
