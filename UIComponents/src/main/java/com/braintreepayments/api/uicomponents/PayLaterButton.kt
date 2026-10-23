package com.braintreepayments.api.uicomponents

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.util.AttributeSet
import androidx.activity.result.ActivityResultCaller
import androidx.appcompat.widget.AppCompatButton
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.uicomponents.PayLaterButtonColor.Companion.fromId

/**
 * A customizable PayPal Pay Later branded button to initiate the PayPal Pay Later flow.
 *
 * This button provides a pre-styled PayPal Pay Later button with configurable colors and handles
 * the complete PayPal payment flow, offering PayPal Pay Later to customers who qualify.
 */
class PayLaterButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    internal var currentStyle: PayLaterButtonColor = PayLaterButtonColor.Blue
        private set
    private val renderer = PaymentButtonRenderer(this, currentStyle.toStyle())
    /**
     * The PayPal client used to create payment auth requests and tokenize results.
     * Must be initialized via [initialize] before use.
     */
    private lateinit var payPalClient: PayPalClient
    private lateinit var payPalLauncher: PayPalLauncher

    /**
     * Callback invoked when the PayPal payment authentication request is launched.
     *
     * This callback is used to receive notifications about the launch status, such as
     * [PayPalPendingRequest.Started] or [PayPalPendingRequest.Failure]. The pending request
     * returned on success should be stored and passed to [handleReturnToApp] to complete the payment flow.
     */
    var payPalLaunchCallback: PayPalLaunchCallback? = null

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.PayLaterButton, 0, 0).apply {
            try {
                currentStyle = fromId(getInt(R.styleable.PayLaterButton_paymentButtonColor, 0))
            } finally {
                recycle()
            }
        }
        contentDescription = context.getString(R.string.pay_later_button_content_description)
        renderer.setStyle(currentStyle.toStyle())
        renderer.setup()
    }

    /**
     * Initializes the PayPal Pay Later button with the required parameters needed to start the payment flow.
     *
     * Call this before setting a PayPal request or handling any payment flows.
     *
     * @param authorization             a Tokenization Key or Client Token used to authenticate
     * @param appLinkReturnUrl          a [Uri] containing the Android App Link website associated with
     * merchant's application to be used to return to merchant's app from the PayPal payment flows.
     * @param deepLinkFallbackUrlScheme a return url scheme that will be used as a deep link fallback when returning to
     * merchant's app via App Link is not available (buyer unchecks the "Open supported links" setting).
     *
     * The value must be a bare URL scheme, not a full URL. It must:
     * - Contain only alphanumeric characters, hyphens (`-`), and periods (`.`)
     * - Not include `://`
     * - Not include a path, query string, or fragment (no `/`, `?`, or `#`)
     * - Match the scheme declared in your `AndroidManifest.xml`
     *
     * Valid examples: `"com.merchant.app.payments"`, `"merchantapp"`, `"merchant-app.payments"`.
     *
     * Invalid examples: `"com.merchant.app.payments://"` (contains `://`),
     * `"com.merchant.app.payments/callback"` (contains a path),
     * `"com.merchant_app"` (underscore not allowed).
     * @param activityResultCaller      an [ActivityResultCaller] (typically a Fragment or Activity) that will be used
     * to register the ActivityResultLauncher for the PayPal browser switch flow.
     */
    fun initialize(
        activityResultCaller: ActivityResultCaller,
        authorization: String,
        appLinkReturnUrl: Uri,
        deepLinkFallbackUrlScheme: String? = null
    ) {

        payPalLauncher = PayPalLauncher(activityResultCaller)
        payPalClient = PayPalClient(
            context = context,
            authorization = authorization,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    }

    /**
     * Sets the PayPal Checkout request configuration and wires up the click handler.
     *
     * PayPal Pay Later is only available with PayPal Checkout. The button sets
     * [PayPalCheckoutRequest.shouldOfferPayLater] to `true` on the provided request.
     *
     * When the button is clicked, it creates a payment auth request and either
     * launches the PayPal flow or notifies the callback of any failures.
     *
     * @param payPalRequest the PayPal Checkout request containing payment details and configuration
     */
    fun setPayPalRequest(payPalRequest: PayPalCheckoutRequest) {
        payPalRequest.shouldOfferPayLater = true
        setOnClickListener {
            if (payPalLaunchCallback == null) {
                throw(NullPointerException("PayPalLaunchCallback must be initialized first"))
            }
            renderer.showLoading()
            payPalClient.createPaymentAuthRequest(
                context = context,
                payPalRequest = payPalRequest
            ) { paymentAuthRequest: PayPalPaymentAuthRequest ->
                when (paymentAuthRequest) {
                    is PayPalPaymentAuthRequest.ReadyToLaunch -> {
                        completePayPalFlow(paymentAuthRequest)
                    }

                    is PayPalPaymentAuthRequest.Failure -> {
                        renderer.hideLoading()
                        payPalLaunchCallback?.onPayPalPaymentAuthRequest(
                            PayPalPendingRequest.Failure(paymentAuthRequest.error)
                        )
                    }
                }
            }
        }
    }

    /**
     * Completes the PayPal flow by launching the authentication and handling the result.
     *
     * Launches the PayPal authentication flow and notifies the [payPalLaunchCallback]
     * with the launch result (started or failure).
     *
     * @param paymentAuthRequest the ready-to-launch payment authentication request
     */
    private fun completePayPalFlow(paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch) {
        getActivity()?.let { activity ->
            val payPalPendingRequest = payPalLauncher.launch(
                activity = activity,
                paymentAuthRequest = paymentAuthRequest
            )
            when (payPalPendingRequest) {
                is PayPalPendingRequest.Started -> {
                    payPalLaunchCallback?.onPayPalPaymentAuthRequest(
                        PayPalPendingRequest.Started(payPalPendingRequest.pendingRequestString)
                    )
                }

                is PayPalPendingRequest.Failure -> {
                    renderer.hideLoading()
                    payPalLaunchCallback?.onPayPalPaymentAuthRequest(
                        PayPalPendingRequest.Failure(payPalPendingRequest.error)
                    )
                }
            }
        } ?: run {
            renderer.hideLoading()
            payPalLaunchCallback?.onPayPalPaymentAuthRequest(
                PayPalPendingRequest.Failure(NullPointerException("Activity is null")))
        }
    }

    /**
     * Handles the return from the PayPal authentication flow and tokenizes the result.
     *
     * Call this method on [onResume] after the user completes the PayPal authentication and is returned to app.
     *
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of the Activity.
     *
     * This method:
     * 1. Extracts the authentication result from the intent
     * 2. Tokenizes the successful authentication
     * 3. Returns the final PayPal result via the callback
     *
     * @param pendingRequest the [PayPalPendingRequest.Started] stored after successfully
     * invoking [PayPalLauncher.launch]
     * @param intent         the intent to return to merchant's application containing a deep link result
     * from the PayPal browser flow
     * @param callback       callback to receive the final PayPal result (success, cancel, or failure)
     */
    fun handleReturnToApp(
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
                    renderer.hideLoading()
                    callback.onPayPalResult(payPalResult)
                }
            }
            is PayPalPaymentAuthResult.NoResult -> {
                renderer.hideLoading()
                callback.onPayPalResult(PayPalResult.Cancel)
            }
            is PayPalPaymentAuthResult.Failure -> {
                renderer.hideLoading()
                callback.onPayPalResult(PayPalResult.Failure(paymentAuthResult.error))
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val (width, height) = renderer.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.onDraw(canvas)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        renderer.updateAppearance()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        renderer.updateAppearance()
    }

    /**
     * Sets the color of the PayPal Pay Later button
     *
     * @property color Value representing the button color. Valid values are BLUE, BLACK, and WHITE
     */
    fun setButtonColor(color: PayLaterButtonColor) {
        if (color == currentStyle) return
        currentStyle = color
        renderer.setStyle(color.toStyle())
    }
}
