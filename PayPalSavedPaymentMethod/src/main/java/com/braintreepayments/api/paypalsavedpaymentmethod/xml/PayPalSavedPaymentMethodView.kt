package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.AttributeSet
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
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalReturnHandling
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodRequest
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodViewModel
import com.braintreepayments.api.paypalsavedpaymentmethod.clearPayPalReturnUrl
import com.braintreepayments.api.paypalsavedpaymentmethod.findActivity
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.openUrl
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.styleFromAttrs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Objects
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

    /** The component's styling. */
    var style: PayPalSavedPaymentMethodViewStyle = styleFromAttrs(context, attrs, defStyleAttr)
        set(value) {
            field = value
            resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, value)
            render()
            // Turning the Pay Later message back on loads it.
            request?.let { viewModel?.onRequestChanged(it, value.showPayPalCreditMessaging) }
        }

    private var resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, style)

    // Creates the view model for these values. Replaced in tests.
    internal var viewModelFactory: (String, Uri, String?) -> ViewModelProvider.Factory =
        { authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme ->
            // The application context, so the client doesn't hold the activity.
            val applicationContext = context.applicationContext
            PayPalSavedPaymentMethodViewModel.Factory {
                PayPalSavedPaymentMethodClient(
                    applicationContext,
                    authorization,
                    appLinkReturnUrl,
                    deepLinkFallbackUrlScheme
                )
            }
        }

    private var viewModel: PayPalSavedPaymentMethodViewModel? = null
    private var payPalLauncher: PayPalLauncher? = null
    private var launchCallback: PayPalSavedPaymentMethodLaunchCallback? = null
    private var tokenizeCallback: PayPalTokenizeCallback? = null
    // Set by initialize; null until then, or after initialize failed.
    private var authorization: String? = null
    private var appLinkReturnUrl: Uri? = null
    private var deepLinkFallbackUrlScheme: String? = null
    // A return handled before the view model is found, completed once it is.
    private var pendingReturn: Pair<PayPalPendingRequest.Started, Intent>? = null
    private var state = PayPalSavedPaymentMethodUiState()
    private var loadingDialog: EditLoadingDialog? = null

    // Identifies this view to its view model, which keeps no reference to the view.
    private val instanceId = UUID.randomUUID().toString()

    // Renders and delivers results only while attached.
    private var attachedScope: CoroutineScope? = null

    init {
        addView(contentView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        contentView.onEditClick = { onEditClick() }
        contentView.onLearnMoreClick = { url -> context.openUrl(url) }
        render()
    }

    /**
     * Sets up the view and loads the funding instrument and the Pay Later message. Calling it again starts over with
     * the new values.
     *
     * @param authorization a client token carrying the buyer's payment method ID.
     * @param appLinkReturnUrl the Android App Link used to return to your app from PayPal.
     * @param payPalCheckoutRequest the checkout the edited funding instrument applies to.
     * @param request the amount and currency for the Pay Later message, and an optional merchant account.
     * @param launchCallback receives the pending request to store when the PayPal paysheet opens.
     * @param tokenizeCallback receives the result of the edit: success with the nonce, cancel, or failure.
     * @param deepLinkFallbackUrlScheme a return URL scheme used when the App Link is not available.
     */
    @Suppress("LongParameterList")
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
                this.authorization = authorization
                this.appLinkReturnUrl = appLinkReturnUrl
                this.deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
                // Inside a fragment's view the view model is found once the view is added to it, otherwise on attach.
                // An attached view in a window with no owner, such as a plain dialog, uses the activity's.
                val owner = findViewTreeViewModelStoreOwner()
                    ?: (context.findActivity() as? ViewModelStoreOwner)?.takeIf { isAttachedToWindow }
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
        if (authorization == null) return false
        val viewModel = viewModel
        if (viewModel == null) {
            pendingReturn = pendingRequest to intent
            return true
        }
        return when (viewModel.handleReturnToApp(pendingRequest.pendingRequestString, payPalLauncher, intent)) {
            PayPalReturnHandling.NOT_TAKEN -> false
            PayPalReturnHandling.TAKEN -> true
            PayPalReturnHandling.COMPLETED -> {
                appLinkReturnUrl?.let { intent.clearPayPalReturnUrl(it, deepLinkFallbackUrlScheme) }
                true
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (authorization != null && viewModel == null) {
            // A window without a view model owner, such as a plain dialog, uses the activity's.
            val owner = findViewTreeViewModelStoreOwner() ?: context.findActivity() as? ViewModelStoreOwner
            // Starts rendering once found.
            owner?.let { bindViewModel(it) }
            return
        }
        val viewModel = viewModel ?: return
        if (viewModel.attachView(instanceId)) observeViewModel(viewModel) else fail(DUPLICATE_VIEW_ID_MESSAGE)
    }

    override fun onDetachedFromWindow() {
        viewModel?.detachView(instanceId)
        attachedScope?.cancel()
        attachedScope = null
        loadingDialog?.dismiss()
        loadingDialog = null
        super.onDetachedFromWindow()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // A host that handles font scale or density changes itself keeps this view, so sizes are resolved again.
        resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, style)
        render()
    }

    /**
     * Uses the view model of the hosting fragment or activity, so the state and an edit in progress survive
     * configuration changes, and the completed edit survives process death. One per view, found by its id; new values
     * from [initialize] get a new one. Completes a return handled before it was found.
     */
    private fun bindViewModel(owner: ViewModelStoreOwner) {
        val authorization = authorization ?: return
        val appLinkReturnUrl = appLinkReturnUrl ?: return
        val factory = viewModelFactory(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)
        // Hashed, since the key is written to the saved instance state and the authorization mustn't be.
        val viewModel = ViewModelProvider(owner, factory)[
            "PayPalSavedPaymentMethodView:$id:" +
                Objects.hash(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme),
            PayPalSavedPaymentMethodViewModel::class.java
        ]
        if (viewModel.isAttachedToAnotherView(instanceId)) {
            fail(DUPLICATE_VIEW_ID_MESSAGE)
            return
        }
        if (viewModel !== this.viewModel) unbindViewModel()
        this.viewModel = viewModel
        request?.let { viewModel.load(it, style.showPayPalCreditMessaging) }
        pendingReturn?.let { (pendingRequest, intent) -> handleReturnToApp(pendingRequest, intent) }
        pendingReturn = null
        if (isAttachedToWindow && viewModel.attachView(instanceId)) observeViewModel(viewModel)
    }

    // Stops showing the current view model.
    private fun unbindViewModel() {
        viewModel?.detachView(instanceId)
        viewModel = null
        attachedScope?.cancel()
        attachedScope = null
    }

    // Hides the view and reports why it can't be used.
    private fun fail(message: String) {
        unbindViewModel()
        authorization = null
        appLinkReturnUrl = null
        deepLinkFallbackUrlScheme = null
        pendingReturn = null
        state = state.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Hidden, isEditing = false)
        render()
        tokenizeCallback?.onPayPalResult(PayPalResult.Failure(BraintreeException(message)))
    }

    // Opens the PayPal paysheet when the buyer taps the funding instrument.
    private val onEditClick: () -> Unit = onEditClick@{
        val viewModel = viewModel ?: return@onEditClick
        val payPalLauncher = payPalLauncher ?: return@onEditClick
        val payPalCheckoutRequest = payPalCheckoutRequest ?: return@onEditClick
        val scope = attachedScope ?: return@onEditClick
        scope.launch {
            when (val pendingRequest = viewModel.startEdit(context, payPalLauncher, payPalCheckoutRequest)) {
                is PayPalPendingRequest.Started -> launchCallback?.onPayPalPendingRequest(pendingRequest)
                is PayPalPendingRequest.Failure ->
                    tokenizeCallback?.onPayPalResult(PayPalResult.Failure(pendingRequest.error))
                null -> Unit
            }
        }
    }

    // Renders every state change and delivers each result, until the view is detached.
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

    private fun render() {
        contentView.render(
            fundingInstrumentState = state.fundingInstrument,
            creditMessagingState = state.creditMessaging,
            resolvedStyle = resolvedStyle,
            editEnabled = !state.isEditing,
            isEditCompleted = state.isEditCompleted
        )
        // Only while attached: a style change while detached mustn't open a dialog nothing would dismiss.
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
