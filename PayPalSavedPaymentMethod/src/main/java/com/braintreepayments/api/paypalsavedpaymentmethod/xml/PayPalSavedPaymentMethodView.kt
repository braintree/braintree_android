package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
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
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.styleFromAttrs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Shows the buyer's saved PayPal funding instrument and an optional Pay Later message, and lets the buyer change the
 * funding instrument in the PayPal paysheet.
 *
 * Call [initialize] once the authorization is available. The host must be an activity with an `ActivityResultRegistry`,
 * such as a `ComponentActivity`. When the buyer taps the funding instrument, the view opens the PayPal paysheet and
 * passes the pending request to the [PayPalSavedPaymentMethodLaunchCallback]. Store it, and when the buyer returns (in
 * `onResume`), pass it to [handleReturnToApp] with the activity's intent. The view tokenizes the PayPal account,
 * delivers the result to the [PayPalTokenizeCallback] and refreshes the funding instrument. Clear the stored request
 * once [handleReturnToApp] returns `true`. A return that was already handled, including one interrupted by process
 * death, isn't completed again, so a result lost to process death isn't delivered.
 *
 * Call all methods and setters on the main thread, and [initialize] before [handleReturnToApp].
 *
 * Requires a client token generated with the buyer's payment method ID. The host activity must call `setIntent(intent)`
 * in `onNewIntent`.
 *
 * The view needs an `android:id` that is stable across recreation (for example from the layout) and not used by another
 * `PayPalSavedPaymentMethodView` in the same activity or fragment. Otherwise [initialize] sends a failure to the
 * [PayPalTokenizeCallback] and the view is hidden. In a fragment, call [initialize] from `onViewCreated` or later. If
 * the screen closes while an edit is being completed, its result isn't delivered.
 *
 * Style it with the `paypalSavedPaymentMethod*` XML attributes or [style]; a style set in code replaces the attributes.
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

    /**
     * The amount and currency for the Pay Later message, and an optional merchant account. A change reloads the
     * message.
     */
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

    /**
     * Creates the view model for these values. The client gets the application context, so it doesn't hold the
     * activity. Replaced in tests.
     */
    internal var viewModelFactory: (String, Uri, String?) -> ViewModelProvider.Factory =
        { authorization, returnUrl, deepLinkScheme ->
            val applicationContext = context.applicationContext
            PayPalSavedPaymentMethodViewModel.Factory {
                PayPalSavedPaymentMethodClient(applicationContext, authorization, returnUrl, deepLinkScheme)
            }
        }

    private var viewModel: PayPalSavedPaymentMethodViewModel? = null
    /** So no other view with the same id shows the view model at the same time. */
    private var attachment: ViewModelAttachment? = null
    private var payPalLauncher: PayPalLauncher? = null
    private var launchCallback: PayPalSavedPaymentMethodLaunchCallback? = null
    private var tokenizeCallback: PayPalTokenizeCallback? = null
    /** What [initialize] was first called with; `null` until then, or after it failed. */
    private var setup: Setup? = null
    /** A return handled before the view model is found, completed once it is. */
    private var pendingReturn: Pair<PayPalPendingRequest.Started, Intent>? = null
    private var state = PayPalSavedPaymentMethodUiState()
    private var loadingDialog: EditLoadingDialog? = null

    /** Identifies this view in [AttachedViews] without keeping a reference to it. */
    private val instanceId = UUID.randomUUID().toString()

    /** Renders and delivers results only while attached. */
    private var attachedScope: CoroutineScope? = null

    init {
        addView(contentView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        contentView.onEditClick = { startEdit() }
        contentView.onLearnMoreClick = { learnMore -> context.openLearnMore(learnMore) }
        render()
    }

    /**
     * Sets up the view and loads the funding instrument and the Pay Later message. Calling it again updates the
     * checkout, the request and the callbacks. The authorization and return URLs from the first call are kept, as on
     * iOS, so a refreshed client token doesn't lose the buyer's edit or a return from PayPal. To start over, for
     * example for another buyer, use a new view with a different `android:id`.
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
        val registryOwner = context.findActivity() as? ActivityResultRegistryOwner
        when {
            registryOwner == null -> fail(NO_ACTIVITY_RESULT_REGISTRY_MESSAGE)
            id == NO_ID -> fail(NO_VIEW_ID_MESSAGE)
            else -> {
                // Registered without a lifecycle, so initialize can be called at any time. Kept on a repeat call, since
                // the paysheet's result goes to the launcher that opened it.
                if (payPalLauncher == null) payPalLauncher = PayPalLauncher(registryOwner.activityResultRegistry)
                if (setup == null) setup = Setup(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)
                val owner = viewModelStoreOwner()
                if (owner != null) bindViewModel(owner) else unbindViewModel()
            }
        }
    }

    /**
     * Completes the edit when the buyer returns from PayPal. Call it from `onResume` with the pending request stored
     * from the [PayPalSavedPaymentMethodLaunchCallback] and the activity's intent. A pending request that was already
     * handled is ignored. A return with no result is reported as [PayPalResult.Cancel]. The PayPal return URL is
     * removed from [intent] once handled.
     *
     * Does nothing before [initialize], so call it first, for example after restoring the authorization in `onCreate`.
     *
     * @return `true` if the view took the return or had already completed it, so the stored [pendingRequest] can be
     * cleared. `false` if it was not taken (before [initialize], or while another edit step is running); keep it and
     * pass it again on the next `onResume`.
     */
    fun handleReturnToApp(pendingRequest: PayPalPendingRequest.Started, intent: Intent): Boolean {
        val payPalLauncher = payPalLauncher ?: return false
        val setup = setup ?: return false
        val viewModel = viewModel
        if (viewModel == null) {
            pendingReturn = pendingRequest to intent
            return true
        }
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
        if (attachment?.attach() == true) observeViewModel(viewModel) else fail(DUPLICATE_VIEW_ID_MESSAGE)
    }

    override fun onDetachedFromWindow() {
        attachment?.detach()
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

    /**
     * Uses the view model of the hosting fragment or activity, so the state and an edit in progress survive
     * configuration changes, and the completed edit survives process death. One per view, found by its id. Completes a
     * return handled before it was found, and starts rendering if the view is attached.
     */
    private fun bindViewModel(owner: ViewModelStoreOwner) {
        val setup = setup ?: return
        val factory = viewModelFactory(setup.authorization, setup.appLinkReturnUrl, setup.deepLinkFallbackUrlScheme)
        val key = "PayPalSavedPaymentMethodView:$id"
        val viewModel = ViewModelProvider(owner, factory)[key, PayPalSavedPaymentMethodViewModel::class.java]
        val attachedViews = ViewModelProvider(owner)[AttachedViews::class.java]
        if (attachedViews.isAttachedToAnotherView(key, instanceId)) {
            fail(DUPLICATE_VIEW_ID_MESSAGE)
            return
        }
        if (viewModel !== this.viewModel) unbindViewModel()
        this.viewModel = viewModel
        val attachment = ViewModelAttachment(attachedViews, key, instanceId)
        this.attachment = attachment
        request?.let { viewModel.load(it, style.showPayPalCreditMessaging) }
        pendingReturn?.let { (pendingRequest, intent) -> handleReturnToApp(pendingRequest, intent) }
        pendingReturn = null
        if (isAttachedToWindow && attachment.attach()) observeViewModel(viewModel)
    }

    /** Stops showing the current view model. */
    private fun unbindViewModel() {
        attachment?.detach()
        attachment = null
        viewModel = null
        attachedScope?.cancel()
        attachedScope = null
    }

    /** Hides the view and reports why it can't be used. */
    private fun fail(message: String) {
        unbindViewModel()
        setup = null
        pendingReturn = null
        state = state.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Hidden, isEditing = false)
        render()
        tokenizeCallback?.onPayPalResult(PayPalResult.Failure(BraintreeException(message)))
    }

    /** Opens the PayPal paysheet when the buyer taps the funding instrument. */
    private fun startEdit() {
        val viewModel = viewModel ?: return
        val payPalLauncher = payPalLauncher ?: return
        val payPalCheckoutRequest = payPalCheckoutRequest ?: return
        val scope = attachedScope ?: return
        scope.launch {
            when (val pendingRequest = viewModel.startEdit(context, payPalLauncher, payPalCheckoutRequest)) {
                is PayPalPendingRequest.Started -> launchCallback?.onPayPalPendingRequest(pendingRequest)
                is PayPalPendingRequest.Failure ->
                    tokenizeCallback?.onPayPalResult(PayPalResult.Failure(pendingRequest.error))
                null -> Unit
            }
        }
    }

    /** Renders every state change and delivers each result, until the view is detached. */
    private fun observeViewModel(viewModel: PayPalSavedPaymentMethodViewModel) {
        attachedScope?.cancel()
        attachedScope = MainScope().also { scope ->
            scope.launch {
                viewModel.state.collect {
                    state = it
                    render()
                }
            }
            scope.launch { viewModel.results.collect { tokenizeCallback?.onPayPalResult(it) } }
        }
    }

    /**
     * Renders the state. The loading dialog shows during an edit only while attached, since one opened while detached,
     * for example by a style change, would never be dismissed.
     */
    private fun render() {
        contentView.render(
            fundingInstrumentState = state.fundingInstrument,
            creditMessagingState = state.creditMessaging,
            resolvedStyle = resolvedStyle,
            editEnabled = !state.isEditing,
            isEditCompleted = state.isEditCompleted
        )
        if (state.isEditing && isAttachedToWindow) {
            if (loadingDialog == null) loadingDialog = EditLoadingDialog(context).also { it.show() }
        } else {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }

    private companion object {
        const val NO_ACTIVITY_RESULT_REGISTRY_MESSAGE =
            "PayPalSavedPaymentMethodView must be hosted in an activity that provides an ActivityResultRegistry."
        const val NO_VIEW_ID_MESSAGE = "PayPalSavedPaymentMethodView needs an android:id."
        const val DUPLICATE_VIEW_ID_MESSAGE =
            "Another PayPalSavedPaymentMethodView in this activity or fragment already uses this android:id."
    }
}

/** The authorization and return URLs, read once, as on iOS. */
private class Setup(val authorization: String, val appLinkReturnUrl: Uri, val deepLinkFallbackUrlScheme: String?)

/**
 * The hosting fragment or activity. Inside a fragment's view it's found once the view is added to the fragment. An
 * attached view in a window with no owner, such as a plain dialog, uses the activity's.
 */
private fun View.viewModelStoreOwner(): ViewModelStoreOwner? = findViewTreeViewModelStoreOwner()
    ?: (context.findActivity() as? ViewModelStoreOwner)?.takeIf { isAttachedToWindow }
